package org.folio.dew.batch;

import static java.util.Objects.isNull;
import static org.folio.dew.domain.dto.JobParameterNames.OUTPUT_FILES_IN_STORAGE;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.config.kafka.KafkaService;
import org.folio.dew.domain.dto.JobParameterNames;
import org.folio.dew.domain.dto.ExportType;
import org.folio.de.entity.Job;
import org.folio.dew.repository.IAcknowledgementRepository;
import org.folio.dew.repository.MinIOObjectStorageRepository;
import org.folio.dew.service.SaveErrorService;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.stereotype.Component;

@Component
@Log4j2
@RequiredArgsConstructor
public class JobCompletionNotificationListener extends JobExecutionListenerSupport {
  private static final String CSV_EXTENSION = ".csv";

  private final IAcknowledgementRepository acknowledgementRepository;
  private final KafkaService kafka;
  private final MinIOObjectStorageRepository repository;
  private final SaveErrorService saveErrorService;

  @Override
  public void beforeJob(JobExecution jobExecution) {
    processJobUpdate(jobExecution, false);
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    processJobUpdate(jobExecution, true);
  }

  private void processJobUpdate(JobExecution jobExecution, boolean after) {
    var jobParameters = jobExecution.getJobParameters();
    var jobId = jobParameters.getString(JobParameterNames.JOB_ID);
    if (StringUtils.isBlank(jobId)) {
      log.error("Job update with empty Job ID {}.", jobExecution);
      return;
    }
    log.info("Job update {}.", jobExecution);

    if (after) {
      if (isBulkEditIdentifiersJob(jobExecution)) {
        String downloadErrorLink = saveErrorService.saveErrorFileAndGetDownloadLink(jobId);
        jobExecution.getExecutionContext().putString(OUTPUT_FILES_IN_STORAGE, saveResult(jobExecution) + (isNull(downloadErrorLink) ? "" : ";" + downloadErrorLink));
        saveErrorService.removeTemporaryErrorStorage(jobId);
      }
      processJobAfter(jobId, jobParameters);
    }

    var jobExecutionUpdate = createJobExecutionUpdate(jobId, jobExecution);

    kafka.send(KafkaService.Topic.JOB_UPDATE, jobExecutionUpdate.getId().toString(), jobExecutionUpdate);
    if (after) {
      log.info("-----------------------------JOB---ENDS-----------------------------");
    }
  }

  private void  processJobAfter(String jobId, JobParameters jobParameters) {
    var acknowledgment = acknowledgementRepository.getAcknowledgement(jobId);
    if (acknowledgment != null) {
      acknowledgment.acknowledge();
      acknowledgementRepository.deleteAcknowledgement(jobId);
    }

    var tempOutputFilePath = jobParameters.getString(JobParameterNames.TEMP_OUTPUT_FILE_PATH);
    if (StringUtils.isBlank(tempOutputFilePath)) {
      return;
    }
    String path = FilenameUtils.getFullPath(tempOutputFilePath);
    String fileNameStart = FilenameUtils.getName(tempOutputFilePath);
    if (StringUtils.isBlank(path) || StringUtils.isBlank(fileNameStart)) {
      return;
    }
    File[] files = new File(path).listFiles((dir, name) -> name.startsWith(fileNameStart));
    if (files == null || files.length <= 0) {
      return;
    }
    for (File f : files) {
      try {
        Files.delete(f.toPath());
      } catch (Exception e) {
        log.error(e.getMessage(), e);
      }
    }
    log.info("Deleted temp files {} of job {}.", files, jobId);
  }

  private Job createJobExecutionUpdate(String jobId, JobExecution jobExecution) {
    var result = new Job();

    result.setId(UUID.fromString(jobId));

    String jobDescription = ExecutionContextUtils.getFromJobExecutionContext(jobExecution, JobParameterNames.JOB_DESCRIPTION);
    if (StringUtils.isNotBlank(jobDescription)) {
      result.setDescription(jobDescription);
    }

    String outputFilesInStorage = ExecutionContextUtils.getFromJobExecutionContext(jobExecution,
        OUTPUT_FILES_IN_STORAGE);
    if (StringUtils.isNotBlank(outputFilesInStorage)) {
      result.setFiles(Arrays.asList(outputFilesInStorage.split(";")));
    }

    result.setStartTime(jobExecution.getStartTime());
    result.setCreatedDate(jobExecution.getCreateTime());
    result.setEndTime(jobExecution.getEndTime());
    result.setUpdatedDate(jobExecution.getLastUpdated());

    List<Throwable> errors = jobExecution.getAllFailureExceptions();
    if (CollectionUtils.isNotEmpty(errors)) {
      result.setErrorDetails(errors.stream().map(t -> {
        t = getThrowableRootCause(t);
        return t.getMessage() + " (" + t.getClass().getSimpleName() + ')';
      }).collect(Collectors.joining("\n")));
    }

    result.setBatchStatus(jobExecution.getStatus());
    result.setExitStatus(jobExecution.getExitStatus());

    return result;
  }

  private Throwable getThrowableRootCause(Throwable t) {
    Throwable cause = t.getCause();
    while (cause != null && cause != t) {
      t = cause;
      cause = t.getCause();
    }
    return t;
  }

  private boolean isBulkEditIdentifiersJob(JobExecution jobExecution) {
    return ExportType.BULK_EDIT_IDENTIFIERS.getValue().equals(jobExecution.getJobInstance().getJobName());
  }

  private String saveResult(JobExecution jobExecution) {
    String path = jobExecution.getJobParameters().getString(JobParameterNames.TEMP_OUTPUT_FILE_PATH);
    try {
      return repository.objectWriteResponseToPresignedObjectUrl(
        repository.uploadObject(FilenameUtils.getName(path) + CSV_EXTENSION, path, null, "text/csv"));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

}
