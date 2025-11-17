package org.folio.dew.batch;

import static java.util.Collections.singletonList;
import static org.folio.dew.batch.ExecutionContextUtils.getFromJobExecutionContext;
import static org.folio.dew.domain.dto.ExportType.AUTH_HEADINGS_UPDATES;
import static org.folio.dew.domain.dto.ExportType.BURSAR_FEES_FINES;
import static org.folio.dew.domain.dto.ExportType.CIRCULATION_LOG;
import static org.folio.dew.domain.dto.ExportType.E_HOLDINGS;
import static org.folio.dew.domain.dto.ExportType.FAILED_LINKED_BIB_UPDATES;
import static org.folio.dew.domain.dto.JobParameterNames.AUTHORITY_CONTROL_FILE_NAME;
import static org.folio.dew.domain.dto.JobParameterNames.BURSAR_FEES_FINES_FILE_NAME;
import static org.folio.dew.domain.dto.JobParameterNames.CIRCULATION_LOG_FILE_NAME;
import static org.folio.dew.domain.dto.JobParameterNames.E_HOLDINGS_FILE_NAME;
import static org.folio.dew.domain.dto.JobParameterNames.OUTPUT_FILES_IN_STORAGE;
import static org.folio.dew.domain.dto.JobParameterNames.TEMP_OUTPUT_FILE_PATH;
import static org.folio.dew.utils.DateTimeHelper.convertToDate;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.de.entity.Job;
import org.folio.dew.config.kafka.KafkaService;
import org.folio.dew.domain.dto.JobParameterNames;
import org.folio.dew.repository.LocalFilesStorage;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;
import org.springframework.stereotype.Component;

@Component
@Log4j2
@RequiredArgsConstructor
public class JobCompletionNotificationListener implements JobExecutionListener {
  private static final String PATHS_DELIMITER = ";";
  private final KafkaService kafka;
  private final LocalFilesStorage localFilesStorage;

  @Override
  public void beforeJob(JobExecution jobExecution) {
    processJobUpdate(jobExecution, false);
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    processJobUpdate(jobExecution, true);
  }

  @SneakyThrows
  private void processJobUpdate(JobExecution jobExecution, boolean after) {
    log.info("processJobUpdate:: process job update with id {}", jobExecution.getJobId());
    var jobParameters = jobExecution.getJobParameters();
    var jobId = jobParameters.getString(JobParameterNames.JOB_ID);
    if (StringUtils.isBlank(jobId)) {
      log.error("Job update with empty Job ID {}.", jobExecution);
      return;
    }

    if (after) {
      processJobAfter(jobId, jobParameters);
    }

    var jobExecutionUpdate = createJobExecutionUpdate(jobId, jobExecution);
    kafka.send(KafkaService.Topic.JOB_UPDATE, jobExecutionUpdate.getId().toString(), jobExecutionUpdate);
    if (after) {
      log.info("-----------------------------JOB---ENDS-----------------------------");
    }
  }

  private void processJobAfter(String jobId, JobParameters jobParameters) {
    var tempOutputFilePath = jobParameters.getString(TEMP_OUTPUT_FILE_PATH);
    if (StringUtils.isBlank(tempOutputFilePath)) {
      return;
    }
    String path = FilenameUtils.getFullPath(tempOutputFilePath);
    String fileNameStart = FilenameUtils.getName(tempOutputFilePath);
    if (StringUtils.isBlank(path) || StringUtils.isBlank(fileNameStart)) {
      return;
    }
    var files = localFilesStorage.walk(path)
      .filter(name -> FilenameUtils.getName(name).startsWith(fileNameStart)).toList();
    if (files.isEmpty()) {
      return;
    }
    for (String f : files) {
        localFilesStorage.delete(f);
    }
    log.info("Deleted temp files {} of job {}.", files, jobId);
  }

  private Job createJobExecutionUpdate(String jobId, JobExecution jobExecution) {
    var result = new Job();

    result.setId(UUID.fromString(jobId));

    var jobDescription = getFromJobExecutionContext(jobExecution, JobParameterNames.JOB_DESCRIPTION);
    if (StringUtils.isNotBlank(jobDescription)) {
      result.setDescription(jobDescription);
    }

    var outputFilesInStorage = getFromJobExecutionContext(jobExecution, OUTPUT_FILES_IN_STORAGE);
    if (StringUtils.isNotBlank(outputFilesInStorage)) {
      result.setFiles(Arrays.asList(outputFilesInStorage.split(PATHS_DELIMITER, 4)));
    }

    var jobName = jobExecution.getJobInstance().getJobName();
    if (jobName.contains(E_HOLDINGS.getValue())) {
      String fileName = getFromJobExecutionContext(jobExecution, E_HOLDINGS_FILE_NAME);
      if (StringUtils.isNotBlank(fileName)) {
        result.setFileNames(singletonList(fileName));
      }
    }

    if (jobName.contains(CIRCULATION_LOG.getValue())) {
      String fileName = getFromJobExecutionContext(jobExecution, CIRCULATION_LOG_FILE_NAME);
      if (StringUtils.isNotBlank(fileName)) {
        result.setFileNames(singletonList(fileName));
      }
    }

    if (jobName.contains(AUTH_HEADINGS_UPDATES.getValue()) || jobName.contains(FAILED_LINKED_BIB_UPDATES.getValue())) {
      String fileName = getFromJobExecutionContext(jobExecution, AUTHORITY_CONTROL_FILE_NAME);
      if (StringUtils.isNotBlank(fileName)) {
        result.setFileNames(singletonList(fileName));
      }
    }

    if (jobName.contains(BURSAR_FEES_FINES.getValue())) {
      String fileName = getFromJobExecutionContext(jobExecution, BURSAR_FEES_FINES_FILE_NAME);
      if (StringUtils.isNotBlank(fileName)) {
        result.setFileNames(singletonList(fileName));
      }
    }

    result.setStartTime(convertToDate(jobExecution.getStartTime()));
    result.setCreatedDate(convertToDate(jobExecution.getCreateTime()));
    result.setEndTime(convertToDate(jobExecution.getEndTime()));
    result.setUpdatedDate(convertToDate(jobExecution.getLastUpdated()));

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
}
