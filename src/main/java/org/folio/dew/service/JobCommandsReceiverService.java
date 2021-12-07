package org.folio.dew.service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.folio.des.config.kafka.KafkaService;
import org.folio.des.domain.JobParameterNames;
import org.folio.des.domain.dto.BursarFeeFines;
import org.folio.des.domain.dto.JobCommand;
import org.folio.dew.batch.ExportJobManager;
import org.folio.dew.batch.bursarfeesfines.service.BursarExportService;
import org.folio.dew.domain.dto.bursarfeesfines.BursarJobPrameterDto;
import org.folio.dew.repository.IAcknowledgementRepository;
import org.folio.dew.repository.MinIOObjectStorageRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.integration.launch.JobLaunchRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import static java.util.Optional.ofNullable;
import static org.folio.des.domain.dto.ExportType.BULK_EDIT_IDENTIFIERS;
import static org.folio.des.domain.dto.ExportType.BULK_EDIT_UPDATE;

@Service
@RequiredArgsConstructor
@Log4j2
public class JobCommandsReceiverService {
  private final ObjectMapper objectMapper;
  private final ExportJobManager exportJobManager;
  private final BursarExportService bursarExportService;
  private final IAcknowledgementRepository acknowledgementRepository;
  private final MinIOObjectStorageRepository objectStorageRepository;
  private final List<Job> jobs;
  private Map<String, Job> jobMap;
  private Map<String, JobCommand> bulkEditJobCommands;
  @Value("${spring.application.name}")
  private String springApplicationName;
  private String workDir;

  @PostConstruct
  public void postConstruct() {
    jobMap = new LinkedHashMap<>();
    for (Job job : jobs) {
      jobMap.put(job.getName(), job);
    }

    workDir = System.getProperty("java.io.tmpdir") + '/' + springApplicationName + '/';
    var file = new File(workDir);
    if (!file.exists()) {
      if (file.mkdir()) {
        log.info("Created working directory {}.", workDir);
      } else {
        throw new IllegalStateException(String.format("Can't create working directory %s.", workDir));
      }
    } else {
      log.info("Working directory {}.", workDir);
    }
    bulkEditJobCommands = new ConcurrentHashMap<>();
  }

  @KafkaListener(
    id = KafkaService.EVENT_LISTENER_ID,
    containerFactory = "kafkaListenerContainerFactory",
    topicPattern = "${application.kafka.topic-pattern}",
    groupId = "${application.kafka.group-id}")
  public void receiveStartJobCommand(JobCommand jobCommand, Acknowledgment acknowledgment) {
    log.info("Received {}.", jobCommand);

    try {
      if (deleteOldFiles(jobCommand, acknowledgment)) {
        return;
      }

      log.info("-----------------------------JOB---STARTS-----------------------------");

      prepareJobParameters(jobCommand);
      acknowledgementRepository.addAcknowledgement(jobCommand.getId().toString(), acknowledgment);

      if (BULK_EDIT_IDENTIFIERS.equals(jobCommand.getExportType()) || BULK_EDIT_UPDATE.equals(jobCommand.getExportType())) {
        addBulkEditJobCommand(jobCommand);
        return;
      }

      var jobLaunchRequest =
        new JobLaunchRequest(
          jobMap.get(jobCommand.getExportType().toString()),
          jobCommand.getJobParameters());

      exportJobManager.launchJob(jobLaunchRequest);
    } catch (Exception e) {
      log.error(e.toString(), e);
    }
  }

  private void prepareJobParameters(JobCommand jobCommand) {
    Map<String, JobParameter> parameters = jobCommand.getJobParameters().getParameters();
    var jobId = jobCommand.getId().toString();
    parameters.put(JobParameterNames.JOB_ID, new JobParameter(jobId));
    var now = new Date();
    parameters.put(JobParameterNames.TEMP_OUTPUT_FILE_PATH,
        new JobParameter(String.format("%s%s_%tF_%tT_%s", workDir, jobCommand.getExportType(), now, now, jobId)));

    normalizeParametersForBursarExport(parameters, jobId);

    jobCommand.setJobParameters(new JobParameters(parameters));
  }

  @SneakyThrows
  private void normalizeParametersForBursarExport(Map<String, JobParameter> parameters, String jobId) {
    final JobParameter bursarFeeFines = parameters.get("bursarFeeFines");
    if (bursarFeeFines == null) {
      return;
    }

    var bff = extractBursarFeeFines(bursarFeeFines);

    BursarJobPrameterDto dto = replaceTypeMappingsCollectionWithHash(bff);
    parameters.put("bursarFeeFines", new JobParameter(objectMapper.writeValueAsString(dto)));

    bursarExportService.addMapping(jobId, bff.getTypeMappings());
  }

  private BursarJobPrameterDto replaceTypeMappingsCollectionWithHash(BursarFeeFines bursarFeeFines) {
    var dto = new BursarJobPrameterDto();
    BeanUtils.copyProperties(bursarFeeFines, dto, "typeMappings");

    dto.setTypeMappings(String.valueOf(bursarFeeFines.getTypeMappings().hashCode()));
    return dto;
  }

  private BursarFeeFines extractBursarFeeFines(JobParameter bursarFeeFines)
    throws com.fasterxml.jackson.core.JsonProcessingException {
    final String value = (String) bursarFeeFines.getValue();
    return objectMapper.readValue(value, BursarFeeFines.class);
  }

  private boolean deleteOldFiles(JobCommand jobCommand, Acknowledgment acknowledgment) {
    if (jobCommand.getType() != JobCommand.Type.DELETE) {
      return false;
    }

    acknowledgment.acknowledge();

    var filesStr = jobCommand.getJobParameters().getString(JobParameterNames.OUTPUT_FILES_IN_STORAGE);
    log.info("Deleting old job files {}.", filesStr);
    if (StringUtils.isEmpty(filesStr)) {
      return true;
    }

    List<String> objects = Arrays.stream(filesStr.split(";")).distinct().map(f -> {
      try {
        return StringUtils.stripStart(new URL(f).getPath(), "/");
      } catch (MalformedURLException e) {
        log.error(e.getMessage(), e);
        return null;
      }
    }).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
    if (!objects.isEmpty()) {
      objectStorageRepository.removeObjects(objects);
    }

    return true;
  }

  public void addBulkEditJobCommand(JobCommand jobCommand) {
    bulkEditJobCommands.put(jobCommand.getId().toString(), jobCommand);
  }

  public Optional<JobCommand> getBulkEditJobCommandById(String id) {
    return ofNullable(bulkEditJobCommands.get(id));
  }

}
