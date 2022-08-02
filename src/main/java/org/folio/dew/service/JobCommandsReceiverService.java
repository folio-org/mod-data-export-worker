package org.folio.dew.service;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import org.folio.dew.batch.ExportJobManagerSync;
import static org.folio.dew.domain.dto.ExportType.BULK_EDIT_IDENTIFIERS;
import static org.folio.dew.domain.dto.ExportType.BULK_EDIT_QUERY;
import static org.folio.dew.domain.dto.ExportType.BULK_EDIT_UPDATE;
import static org.folio.dew.domain.dto.ExportType.CIRCULATION_LOG;
import static org.folio.dew.domain.dto.ExportType.EDIFACT_ORDERS_EXPORT;
import static org.folio.dew.utils.Constants.CSV_EXTENSION;
import static org.folio.dew.utils.Constants.FILE_NAME;
import static org.folio.dew.domain.dto.ExportType.E_HOLDINGS;

import java.io.File;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.de.entity.JobCommand;
import org.folio.dew.batch.ExportJobManager;
import org.folio.dew.batch.bursarfeesfines.service.BursarExportService;
import org.folio.dew.client.SearchClient;
import org.folio.dew.config.kafka.KafkaService;
import org.folio.dew.domain.dto.BursarFeeFines;
import org.folio.dew.domain.dto.JobParameterNames;
import org.folio.dew.domain.dto.bursarfeesfines.BursarJobPrameterDto;
import org.folio.dew.error.FileOperationException;
import org.folio.dew.repository.IAcknowledgementRepository;
import org.folio.dew.repository.MinIOObjectStorageRepository;
import org.folio.spring.scope.FolioExecutionScopeExecutionContextManager;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.integration.launch.JobLaunchRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class JobCommandsReceiverService {

  private final ObjectMapper objectMapper;
  private final ExportJobManager exportJobManager;
  private final ExportJobManagerSync exportJobManagerSync;
  private final BursarExportService bursarExportService;
  private final IAcknowledgementRepository acknowledgementRepository;
  private final MinIOObjectStorageRepository remoteObjectStorageRepository;
  private final BulkEditProcessingErrorsService bulkEditProcessingErrorsService;
  private final SearchClient searchClient;
  private final FileNameResolver fileNameResolver;
  private final MinIOObjectStorageRepository minIOObjectStorageRepository;
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
    concurrency = "30",
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

      if (Set.of(BULK_EDIT_IDENTIFIERS, BULK_EDIT_QUERY, BULK_EDIT_UPDATE).contains(jobCommand.getExportType())) {
        addBulkEditJobCommand(jobCommand);
        if (BULK_EDIT_IDENTIFIERS.equals(jobCommand.getExportType()) || BULK_EDIT_UPDATE.equals(jobCommand.getExportType())) {
          acknowledgementRepository.addAcknowledgement(jobCommand.getId().toString(), acknowledgment);
          FolioExecutionScopeExecutionContextManager.endFolioExecutionContext();
          log.debug("FOLIO context closed.");
          return;
        }
      }

      var jobLaunchRequest =
        new JobLaunchRequest(
          jobMap.get(resolveJobKey(jobCommand)),
          jobCommand.getJobParameters());

      acknowledgementRepository.addAcknowledgement(jobCommand.getId().toString(), acknowledgment);
      exportJobManagerSync.launchJob(jobLaunchRequest);

    } catch (Exception e) {
      log.error(e.toString(), e);
    }
  }

  private String resolveJobKey(JobCommand jobCommand) {
    if (jobCommand.getExportType().equals(BULK_EDIT_QUERY)) {
      return BULK_EDIT_QUERY + "-" + jobCommand.getEntityType();
    }
    return jobCommand.getExportType().toString();
  }

  private void prepareJobParameters(JobCommand jobCommand) {
    var paramsBuilder = new JobParametersBuilder(jobCommand.getJobParameters());

    // TODO enrich exportType.json with value MARC_EXPORT
    if ("MARC_EXPORT".equals(jobCommand.getExportType().getValue())) {
      var uploadedFilePath = jobCommand.getJobParameters().getString(FILE_NAME);
      if (nonNull(uploadedFilePath) && FilenameUtils.isExtension(uploadedFilePath, "cql")) {
        var tempIdentifiersFileName = workDir + FilenameUtils.getBaseName(uploadedFilePath) + CSV_EXTENSION;
        try (var lines = Files.lines(Path.of(uploadedFilePath));
             var outputStream = new FileOutputStream(tempIdentifiersFileName)) {
          var query = lines.collect(Collectors.joining());
          // TODO enrich entityType.json with values INSTANCE, HOLDINGS
          InputStreamResource resource = null;
          if ("INSTANCE".equals(jobCommand.getEntityType().getValue())) {
            resource = searchClient.getInstanceIds(query).getBody();
          } else if ("HOLDINGS".equals(jobCommand.getEntityType().getValue())) {
            resource = searchClient.getHoldingIds(query).getBody();
          }
          if (nonNull(resource)) {
            resource.getInputStream().transferTo(outputStream);
          }
          var identifiersUrl = minIOObjectStorageRepository.objectWriteResponseToPresignedObjectUrl(
            minIOObjectStorageRepository.uploadObject(FilenameUtils.getName(tempIdentifiersFileName), tempIdentifiersFileName, null, "text/csv", true));
          paramsBuilder.addString(FILE_NAME, identifiersUrl);
        } catch (Exception e) {
          var msg = String.format("Failed to read %s, reason: %s", FilenameUtils.getBaseName(uploadedFilePath), e.getMessage());
          log.error(msg);
          throw new FileOperationException(msg);
        }
      }
    }

    var jobId = jobCommand.getId().toString();
    var outputFileName = fileNameResolver.resolve(jobCommand, workDir, jobId);

    paramsBuilder.addString(JobParameterNames.JOB_ID, jobId);
    paramsBuilder.addString(JobParameterNames.TEMP_OUTPUT_FILE_PATH, outputFileName);

    addOrderExportSpecificParameters(jobCommand, paramsBuilder);

    normalizeParametersForBursarExport(paramsBuilder, jobId);

    jobCommand.setJobParameters(paramsBuilder.toJobParameters());
  }

  private void addOrderExportSpecificParameters(JobCommand jobCommand, JobParametersBuilder paramsBuilder) {
    if (jobCommand.getExportType().equals(EDIFACT_ORDERS_EXPORT)) {
      paramsBuilder.addString(JobParameterNames.JOB_NAME, jobCommand.getName());
    }
  }

  @SneakyThrows
  private void normalizeParametersForBursarExport(JobParametersBuilder paramsBuilder, String jobId) {
    final JobParameter bursarFeeFines = paramsBuilder.toJobParameters().getParameters().get("bursarFeeFines");
    if (bursarFeeFines == null) {
      return;
    }

    var bff = extractBursarFeeFines(bursarFeeFines);

    BursarJobPrameterDto dto = replaceTypeMappingsCollectionWithHash(bff);
    paramsBuilder.addString("bursarFeeFines", objectMapper.writeValueAsString(dto));

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
      remoteObjectStorageRepository.removeObjects(objects);
    }
    bulkEditJobCommands.remove(jobCommand.getId().toString());
    bulkEditProcessingErrorsService.removeTemporaryErrorStorage(jobCommand.getId().toString());
    return true;
  }

  public void addBulkEditJobCommand(JobCommand jobCommand) {
    bulkEditJobCommands.putIfAbsent(jobCommand.getId().toString(), jobCommand);
  }

  public Optional<JobCommand> getBulkEditJobCommandById(String id) {
    return ofNullable(bulkEditJobCommands.get(id));
  }

}
