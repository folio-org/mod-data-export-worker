package org.folio.dew.batch;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.dew.domain.dto.ExportType.BULK_EDIT_IDENTIFIERS;
import static org.folio.dew.domain.dto.ExportType.BULK_EDIT_UPDATE;
import static org.folio.dew.domain.dto.JobParameterNames.OUTPUT_FILES_IN_STORAGE;
import static org.folio.dew.domain.dto.JobParameterNames.TOTAL_RECORDS;
import static org.folio.dew.domain.dto.JobParameterNames.UPDATED_FILE_NAME;
import static org.folio.dew.domain.dto.JobParameterNames.TEMP_OUTPUT_FILE_PATH;
import static org.folio.dew.utils.Constants.MATCHED_RECORDS;
import static org.folio.dew.utils.Constants.CHANGED_RECORDS;
import static org.folio.dew.utils.Constants.FILE_NAME;
import static org.folio.dew.utils.Constants.CSV_EXTENSION;
import static org.folio.dew.utils.Constants.UPDATED_PREFIX;
import static org.folio.dew.utils.Constants.EXPORT_TYPE;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.de.entity.Job;
import org.folio.dew.config.kafka.KafkaService;
import org.folio.dew.domain.dto.JobParameterNames;
import org.folio.dew.domain.dto.Progress;
import org.folio.dew.domain.dto.UserFormat;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.repository.IAcknowledgementRepository;
import org.folio.dew.repository.MinIOObjectStorageRepository;
import org.folio.dew.service.BulkEditChangedRecordsService;
import org.folio.dew.service.BulkEditProcessingErrorsService;
import org.folio.dew.service.BulkEditStatisticService;
import org.folio.dew.utils.CsvHelper;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
@RequiredArgsConstructor
public class JobCompletionNotificationListener extends JobExecutionListenerSupport {
  private static final String PATHS_DELIMITER = ";";
  private static final int COMPLETE_PROGRESS_VALUE = 100;

  private final IAcknowledgementRepository acknowledgementRepository;
  private final KafkaService kafka;
  private final MinIOObjectStorageRepository repository;
  private final BulkEditProcessingErrorsService bulkEditProcessingErrorsService;
  private final BulkEditStatisticService bulkEditStatisticService;
  private final BulkEditChangedRecordsService changedRecordsService;

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
    var jobParameters = jobExecution.getJobParameters();
    var jobId = jobParameters.getString(JobParameterNames.JOB_ID);
    if (StringUtils.isBlank(jobId)) {
      log.error("Job update with empty Job ID {}.", jobExecution);
      return;
    }
    log.info("Job update {}.", jobExecution);

    if (after) {
      if (isBulkEditIdentifiersJob(jobExecution)) {
        handleProcessingErrors(jobExecution, jobId);
      }
      if (isBulkEditUpdateJob(jobExecution)) {
        handleProcessingChangedRecords(jobExecution);
      }
      if (isBulkEditUpdateJob(jobExecution)) {
        String downloadErrorLink = bulkEditProcessingErrorsService.saveErrorFileAndGetDownloadLink(jobId);
        var isChangedRecordsLinkPresent = jobExecution.getExecutionContext().containsKey(OUTPUT_FILES_IN_STORAGE);
        jobExecution.getExecutionContext().putString(OUTPUT_FILES_IN_STORAGE,
          (isChangedRecordsLinkPresent ? jobExecution.getExecutionContext().getString(OUTPUT_FILES_IN_STORAGE) : EMPTY) +
            PATHS_DELIMITER + (StringUtils.isNotBlank(downloadErrorLink) ? downloadErrorLink : EMPTY));

      }
      processJobAfter(jobId, jobParameters);
    } else {
      if (jobExecution.getJobInstance().getJobName().contains(BULK_EDIT_UPDATE.getValue())) {
        String updatedFilePath = jobExecution.getJobParameters().getString(UPDATED_FILE_NAME);
        String filePath = requireNonNull(isNull(updatedFilePath) ? jobExecution.getJobParameters().getString(FILE_NAME) : updatedFilePath);
        try (var lines = Files.lines(Paths.get(filePath))) {
          int totalUsers = (int) lines.count() - 1;
          jobExecution.getExecutionContext().putLong(TOTAL_RECORDS, totalUsers);
        } catch (NullPointerException e) {
          String msg = String.format("Couldn't open a required for the job file. File path '%s'", FILE_NAME);
          log.debug(msg);
          throw new BulkEditException(msg);
        }
      }
    }

    var jobExecutionUpdate = createJobExecutionUpdate(jobId, jobExecution);
    if (jobExecution.getJobInstance().getJobName().contains(BULK_EDIT_UPDATE.getValue()) || jobExecution.getJobInstance().getJobName().contains(BULK_EDIT_IDENTIFIERS.getValue())) {
      var progress = new Progress();
      if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
        var fileName = FilenameUtils.getName(jobParameters.getString(FILE_NAME));
        var errors = bulkEditProcessingErrorsService.readErrorsFromCSV(jobId, fileName, 1_000_000);
        var statistic = bulkEditStatisticService.getStatistic();
        var totalRecords = statistic.getSuccess() + errors.getTotalRecords();
        progress.setTotal(totalRecords);
        progress.setProcessed(totalRecords);
        progress.setProgress(COMPLETE_PROGRESS_VALUE);
        progress.setSuccess(statistic.getSuccess());
        progress.setErrors(errors.getTotalRecords());
        jobExecutionUpdate.setProgress(progress);
      }
      jobExecutionUpdate.setProgress(progress);
    }

    kafka.send(KafkaService.Topic.JOB_UPDATE, jobExecutionUpdate.getId().toString(), jobExecutionUpdate);
    if (after) {
      log.info("-----------------------------JOB---ENDS-----------------------------");
    }
  }

  private void handleProcessingErrors(JobExecution jobExecution, String jobId) {
    String downloadErrorLink = bulkEditProcessingErrorsService.saveErrorFileAndGetDownloadLink(jobId);
    jobExecution.getExecutionContext().putString(OUTPUT_FILES_IN_STORAGE, saveResult(jobExecution) + (isNull(downloadErrorLink) ? EMPTY : PATHS_DELIMITER + downloadErrorLink));
  }

  private void handleProcessingChangedRecords(JobExecution jobExecution) {
    jobExecution.getExecutionContext().putString(OUTPUT_FILES_IN_STORAGE, saveResult(jobExecution));
  }

  private void processJobAfter(String jobId, JobParameters jobParameters) {
    var acknowledgment = acknowledgementRepository.getAcknowledgement(jobId);
    if (acknowledgment != null) {
      acknowledgment.acknowledge();
      acknowledgementRepository.deleteAcknowledgement(jobId);
    }

    var tempOutputFilePath = jobParameters.getString(TEMP_OUTPUT_FILE_PATH);
    if (StringUtils.isBlank(tempOutputFilePath) ||
      jobParameters.getParameters().containsKey(EXPORT_TYPE) && jobParameters.getString(EXPORT_TYPE).equals(BULK_EDIT_UPDATE.getValue())) {
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
      result.setFiles(Arrays.asList(outputFilesInStorage.split(PATHS_DELIMITER)));
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
    return jobExecution.getJobInstance().getJobName().contains(BULK_EDIT_IDENTIFIERS.getValue());
  }

  private boolean isBulkEditUpdateJob(JobExecution jobExecution) {
    return jobExecution.getJobInstance().getJobName().contains(BULK_EDIT_UPDATE.getValue());
  }

  private boolean isBulkEditContentUpdateJob(JobExecution jobExecution) {
    return nonNull(jobExecution.getJobParameters().getString(UPDATED_FILE_NAME));
  }

  private String saveResult(JobExecution jobExecution) {
    var path = preparePath(jobExecution);
    try {
      if (isEmpty(path) || noRecordsFound(path)) {
        return EMPTY; // To prevent downloading empty file.
      }
      return repository.objectWriteResponseToPresignedObjectUrl(
        repository.uploadObject(prepareObject(jobExecution, path), path, prepareDownloadFilename(jobExecution, path), "text/csv", !isBulkEditUpdateJob(jobExecution)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private String preparePath(JobExecution jobExecution) {
    if (isBulkEditContentUpdateJob(jobExecution)) {
      return jobExecution.getJobParameters().getString(UPDATED_FILE_NAME);
    } else if (isBulkEditUpdateJob(jobExecution)) {
      return prepareChangedUsersFile(jobExecution.getJobParameters().getString(FILE_NAME), jobExecution.getJobParameters().getString(JobParameterNames.JOB_ID));
    }
    return jobExecution.getJobParameters().getString(TEMP_OUTPUT_FILE_PATH);
  }
  private boolean noRecordsFound(String path) throws IOException {
    Path pathToFoundRecords = Path.of(path);
    if (Files.notExists(pathToFoundRecords)) {
      log.error("Path to found records does not exist: {}", path);
      return true;
    }
    return Files.lines(pathToFoundRecords).count() <= 1;
  }

  private String prepareObject(JobExecution jobExecution, String path) {
    return FilenameUtils.getName(path) + (!isBulkEditUpdateJob(jobExecution) ? CSV_EXTENSION : EMPTY);
  }

  private String prepareDownloadFilename(JobExecution jobExecution, String path) {
    if (isBulkEditIdentifiersJob(jobExecution)) {
      return null;
    }
    return FilenameUtils.getName(path).replace(MATCHED_RECORDS, CHANGED_RECORDS).replace(UPDATED_PREFIX, EMPTY);
  }

  private String prepareChangedUsersFile(String path, String jobId) {
    var updatedIds = changedRecordsService.fetchChangedUserIds(jobId);
    if (isNull(updatedIds) || updatedIds.isEmpty()) {
      return EMPTY;
    }
    try {
      var updatedUserFormats = CsvHelper.readRecordsFromFile(path, UserFormat.class, true)
        .stream()
        .filter(userFormat -> updatedIds.contains(userFormat.getId()))
        .collect(Collectors.toList());
      CsvHelper.saveRecordsToCsv(updatedUserFormats, UserFormat.class, path);
    } catch (Exception e) {
      log.error("Error processing file {}: {}", path, e.getMessage());
    }
    return path;
  }

}
