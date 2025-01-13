package org.folio.dew.service;

import static org.folio.dew.domain.dto.ExportType.BULK_EDIT_IDENTIFIERS;
import static org.folio.dew.domain.dto.ExportType.BULK_EDIT_QUERY;
import static org.folio.dew.domain.dto.ExportType.CLAIMS;
import static org.folio.dew.domain.dto.ExportType.EDIFACT_ORDERS_EXPORT;
import static org.folio.dew.utils.Constants.BULKEDIT_DIR_NAME;
import static org.folio.dew.utils.Constants.getWorkingDirectory;

import jakarta.annotation.PostConstruct;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.de.entity.JobCommand;
import org.folio.de.entity.JobCommandType;
import org.folio.dew.batch.ExportJobManagerSync;
import org.folio.dew.batch.acquisitions.services.ResendService;
import org.folio.dew.client.SearchClient;
import org.folio.dew.config.kafka.KafkaService;
import org.folio.dew.domain.dto.JobParameterNames;
import org.folio.dew.repository.JobCommandRepository;
import org.folio.dew.repository.LocalFilesStorage;
import org.folio.dew.repository.RemoteFilesStorage;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.integration.launch.JobLaunchRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class JobCommandsReceiverService {
  private static final boolean JOB_PARAMETER_DEFAULT_IDENTIFYING_VALUE = false;

  private final FolioModuleMetadata folioModuleMetadata;

  private final ExportJobManagerSync exportJobManagerSync;
  private final RemoteFilesStorage remoteFilesStorage;
  private final LocalFilesStorage localFilesStorage;
  private final BulkEditProcessingErrorsService bulkEditProcessingErrorsService;
  private final SearchClient searchClient;
  private final FileNameResolver fileNameResolver;
  private final JobCommandRepository jobCommandRepository;
  private final ResendService resendService;
  private final List<Job> jobs;
  private Map<String, Job> jobMap;
  @Value("${spring.application.name}")
  private String springApplicationName;
  private String workDir;

  @PostConstruct
  public void postConstruct() {
    jobMap = new LinkedHashMap<>();
    for (Job job : jobs) {
      jobMap.put(job.getName(), job);
    }

    workDir = getWorkingDirectory(springApplicationName, BULKEDIT_DIR_NAME);
  }

  @KafkaListener(
    concurrency = "${spring.kafka.listener.concurrency}",
    id = KafkaService.EVENT_LISTENER_ID,
    containerFactory = "kafkaListenerContainerFactory",
    topicPattern = "${application.kafka.topic-pattern}",
    groupId = "${application.kafka.group-id}")
  public void receiveStartJobCommand(@Payload JobCommand jobCommand, @Headers Map<String, Object> messageHeaders) {
    var defaultFolioExecutionContext = DefaultFolioExecutionContext.fromMessageHeaders(folioModuleMetadata, messageHeaders);

    try (var context = new FolioExecutionContextSetter(defaultFolioExecutionContext)) {
      log.info("Received {}.", jobCommand);

      try {
        if (JobCommandType.RESEND.equals(jobCommand.getType())) {
          resendService.resendExportedFile(jobCommand);
          return;
        }

        if (deleteOldFiles(jobCommand)) {
          return;
        }
        log.info("-----------------------------JOB---STARTS-----------------------------");

        prepareJobParameters(jobCommand);

        if (Set.of(BULK_EDIT_IDENTIFIERS, BULK_EDIT_QUERY).contains(jobCommand.getExportType())) {
          addBulkEditJobCommand(jobCommand);
          if (BULK_EDIT_IDENTIFIERS.equals(jobCommand.getExportType())) {
            return;
          }
        }

        var jobKey = resolveJobKey(jobCommand);
        log.info("receiveStartJobCommand:: Resolving job with key: '{}' for command: '{}'", jobKey, jobCommand.getId());
        var job = jobMap.get(jobKey);

        if (job != null) {
          log.info("receiveStartJobCommand:: Job resolved: '{}', launching...", job.getName());
          exportJobManagerSync.launchJob(new JobLaunchRequest(job, jobCommand.getJobParameters()));
        } else {
          log.error("Job with key '{}' not found for command: '{}'", jobKey, jobCommand.getId());
        }

      } catch (Exception e) {
        log.error(e.toString(), e);
      }
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

    var jobId = jobCommand.getId().toString();
    var outputFileName = fileNameResolver.resolve(jobCommand, workDir, jobId);

    paramsBuilder.addString(JobParameterNames.JOB_ID, jobId);
    paramsBuilder.addString(JobParameterNames.TEMP_OUTPUT_FILE_PATH, outputFileName, JOB_PARAMETER_DEFAULT_IDENTIFYING_VALUE);

    addOrderExportSpecificParameters(jobCommand, paramsBuilder);

    jobCommand.setJobParameters(paramsBuilder.toJobParameters());
  }

  private void addOrderExportSpecificParameters(JobCommand jobCommand, JobParametersBuilder paramsBuilder) {
    if (jobCommand.getExportType() == EDIFACT_ORDERS_EXPORT || jobCommand.getExportType() == CLAIMS) {
      paramsBuilder.addString(JobParameterNames.JOB_NAME, jobCommand.getName(), JOB_PARAMETER_DEFAULT_IDENTIFYING_VALUE);
    }
  }

  private boolean deleteOldFiles(JobCommand jobCommand) {
    if (jobCommand.getType() != JobCommandType.DELETE) {
      return false;
    }

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
      remoteFilesStorage.removeObjects(objects);
    }
    jobCommandRepository.delete(jobCommand);
    bulkEditProcessingErrorsService.removeTemporaryErrorStorage();
    return true;
  }

  public void addBulkEditJobCommand(JobCommand jobCommand) {
    if (!jobCommandRepository.existsById(jobCommand.getId())) jobCommandRepository.save(jobCommand);
  }

  public Optional<JobCommand> getBulkEditJobCommandById(String id) {
    return jobCommandRepository.findById(UUID.fromString(id));
  }

  public void updateJobCommand(JobCommand jobCommand) {
    jobCommandRepository.save(jobCommand);
  }
}
