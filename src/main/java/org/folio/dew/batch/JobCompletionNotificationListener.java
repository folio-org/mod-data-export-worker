package org.folio.dew.batch;

import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.dew.batch.ExecutionContextUtils.getFromJobExecutionContext;
import static org.folio.dew.domain.dto.ExportType.AUTH_HEADINGS_UPDATES;
import static org.folio.dew.domain.dto.ExportType.BULK_EDIT_IDENTIFIERS;
import static org.folio.dew.domain.dto.ExportType.BULK_EDIT_QUERY;
import static org.folio.dew.domain.dto.ExportType.BULK_EDIT_UPDATE;
import static org.folio.dew.domain.dto.ExportType.BURSAR_FEES_FINES;
import static org.folio.dew.domain.dto.ExportType.CIRCULATION_LOG;
import static org.folio.dew.domain.dto.ExportType.E_HOLDINGS;
import static org.folio.dew.domain.dto.ExportType.FAILED_LINKED_BIB_UPDATES;
import static org.folio.dew.domain.dto.JobParameterNames.AUTHORITY_CONTROL_FILE_NAME;
import static org.folio.dew.domain.dto.JobParameterNames.BURSAR_FEES_FINES_FILE_NAME;
import static org.folio.dew.domain.dto.JobParameterNames.CIRCULATION_LOG_FILE_NAME;
import static org.folio.dew.domain.dto.JobParameterNames.E_HOLDINGS_FILE_NAME;
import static org.folio.dew.domain.dto.JobParameterNames.OUTPUT_FILES_IN_STORAGE;
import static org.folio.dew.domain.dto.JobParameterNames.TEMP_LOCAL_FILE_PATH;
import static org.folio.dew.domain.dto.JobParameterNames.TEMP_LOCAL_MARC_PATH;
import static org.folio.dew.domain.dto.JobParameterNames.TEMP_OUTPUT_FILE_PATH;
import static org.folio.dew.domain.dto.JobParameterNames.TEMP_OUTPUT_MARC_PATH;
import static org.folio.dew.domain.dto.JobParameterNames.TOTAL_RECORDS;
import static org.folio.dew.domain.dto.JobParameterNames.UPDATED_FILE_NAME;
import static org.folio.dew.utils.BulkEditProcessorHelper.convertToDate;
import static org.folio.dew.utils.Constants.CHANGED_RECORDS;
import static org.folio.dew.utils.Constants.CSV_EXTENSION;
import static org.folio.dew.utils.Constants.EXPORT_TYPE;
import static org.folio.dew.utils.Constants.FILE_NAME;
import static org.folio.dew.utils.Constants.INITIAL_PREFIX;
import static org.folio.dew.utils.Constants.MATCHED_RECORDS;
import static org.folio.dew.utils.Constants.PATH_SEPARATOR;
import static org.folio.dew.utils.Constants.PATH_TO_ERRORS;
import static org.folio.dew.utils.Constants.PATH_TO_MATCHED_RECORDS;
import static org.folio.dew.utils.Constants.TEMP_IDENTIFIERS_FILE_NAME;
import static org.folio.dew.utils.Constants.UPDATED_PREFIX;
import static org.folio.dew.utils.SystemHelper.validatePath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
import org.folio.dew.domain.dto.ErrorType;
import org.folio.dew.domain.dto.JobParameterNames;
import org.folio.dew.domain.dto.Progress;
import org.folio.dew.domain.dto.UserFormat;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.repository.LocalFilesStorage;
import org.folio.dew.repository.RemoteFilesStorage;
import org.folio.dew.service.BulkEditProcessingErrorsService;
import org.folio.dew.service.BulkEditStatisticService;
import org.folio.dew.utils.CsvHelper;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;
import org.springframework.stereotype.Component;

@Component
@Log4j2
@RequiredArgsConstructor
public class JobCompletionNotificationListener implements JobExecutionListener {
  private static final String PATHS_DELIMITER = ";";
  private static final int COMPLETE_PROGRESS_VALUE = 100;
  private final KafkaService kafka;
  private final RemoteFilesStorage remoteFilesStorage;
  private final LocalFilesStorage localFilesStorage;
  private final BulkEditProcessingErrorsService bulkEditProcessingErrorsService;
  private final BulkEditStatisticService bulkEditStatisticService;

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
      if (isBulkEditIdentifiersJob(jobExecution)) {
        moveTemporaryFilesToStorage(jobParameters);
        handleProcessingMatchedRecordsAndErrors(jobExecution, jobId);
        handleProcessingMarcFile(jobExecution);
      }
      processJobAfter(jobId, jobParameters);
    } else {
      if (jobExecution.getJobInstance().getJobName().contains(BULK_EDIT_UPDATE.getValue())) {
        String updatedFilePath = jobExecution.getJobParameters().getString(UPDATED_FILE_NAME);
        String filePath = requireNonNull(isNull(updatedFilePath) ? jobExecution.getJobParameters().getString(FILE_NAME) : updatedFilePath);
        if (localFilesStorage.notExists(filePath) && remoteFilesStorage.containsFile(filePath)) {
          int totalUsers = CsvHelper.readRecordsFromStorage(remoteFilesStorage, filePath, UserFormat.class, true).size();
          jobExecution.getExecutionContext().putInt(TOTAL_RECORDS, totalUsers);
        } else {
          try (var lines = localFilesStorage.lines(filePath)) {
            int totalUsers = (int) lines.count() - 1;
            jobExecution.getExecutionContext().putInt(TOTAL_RECORDS, totalUsers);
          } catch (NullPointerException e) {
            String msg = String.format("Couldn't open a required for the job file. File path '%s'", FILE_NAME);
            log.debug(msg);
            throw new BulkEditException(msg, ErrorType.ERROR);
          }
        }
      }
    }

    var jobExecutionUpdate = createJobExecutionUpdate(jobId, jobExecution);
    if (jobExecution.getJobInstance().getJobName().contains(BULK_EDIT_IDENTIFIERS.getValue())) {
      var progress = new Progress();
      if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
        var fileName = FilenameUtils.getName(jobParameters.getString(FILE_NAME));
        var errors = bulkEditProcessingErrorsService.readErrorsFromCSV(jobId, fileName, 1_000_000);
        var totalRecords = bulkEditStatisticService.getSuccess(jobId) + errors.getTotalErrorRecords();
        progress.setTotal(totalRecords);
        progress.setProcessed(totalRecords);
        progress.setProgress(COMPLETE_PROGRESS_VALUE);
        progress.setSuccess(bulkEditStatisticService.getSuccess(jobId));
        progress.setErrors(errors.getTotalErrorRecords());
        progress.setWarnings(errors.getTotalWarningRecords());
        jobExecutionUpdate.setProgress(progress);
        bulkEditStatisticService.reset(jobId);
      }
      jobExecutionUpdate.setProgress(progress);
    }
    if (isBulkEditJob(jobExecution)) {
      populateFileNames(jobExecution, jobExecutionUpdate, jobParameters);
    }
    kafka.send(KafkaService.Topic.JOB_UPDATE, jobExecutionUpdate.getId().toString(), jobExecutionUpdate);
    if (after) {
      log.info("-----------------------------JOB---ENDS-----------------------------");
    }
  }

  private void populateFileNames(JobExecution jobExecution, Job jobExecutionUpdate, JobParameters jobParameters) {
    var fileNames = jobExecutionUpdate.getFileNames();
    if (isNull(fileNames)) {
      fileNames = new ArrayList<>();
      jobExecutionUpdate.setFileNames(fileNames);
    }
    if (jobExecution.getExecutionContext().containsKey(PATH_TO_ERRORS)) {
      fileNames.add(jobExecution.getExecutionContext().getString(PATH_TO_ERRORS));
    }
    if (jobExecution.getExecutionContext().containsKey(PATH_TO_MATCHED_RECORDS)) {
      fileNames.add(jobExecution.getExecutionContext().getString(PATH_TO_MATCHED_RECORDS));
    }
  }

  private void moveTemporaryFilesToStorage(JobParameters jobParameters) throws IOException {
    var tmpFileName = jobParameters.getString(TEMP_LOCAL_FILE_PATH);
    if (nonNull(tmpFileName)) {
      moveFileToStorage(jobParameters.getString(TEMP_OUTPUT_FILE_PATH), tmpFileName);
      moveFileToStorage(jobParameters.getString(TEMP_OUTPUT_FILE_PATH) + ".json", tmpFileName + ".json");
    }
    var tmpMarcName = jobParameters.getString(TEMP_LOCAL_MARC_PATH);
    if (nonNull(tmpMarcName)) {
      tmpMarcName += ".mrc";
      if (Files.exists(Path.of(tmpMarcName))) {
        moveFileToStorage(jobParameters.getString(TEMP_OUTPUT_MARC_PATH) + ".mrc", tmpMarcName);
      }
    }

    var tmpIdentifiersFileName = jobParameters.getString(TEMP_IDENTIFIERS_FILE_NAME);
    if (nonNull(tmpIdentifiersFileName) && Files.deleteIfExists(Path.of(tmpIdentifiersFileName))) {
      log.info("Deleted temporary identifiers file: {}", tmpIdentifiersFileName);
    }
  }

  private void moveFileToStorage(String destFileName, String sourceFileName) throws IOException {
    var sourcePath = Path.of(sourceFileName);
    if (Files.exists(sourcePath)) {
      localFilesStorage.writeFile(destFileName, sourcePath);
      if (Files.deleteIfExists(sourcePath)) {
        log.info("Deleted temporary file: {}", sourceFileName);
      }
    }
  }

  private void handleProcessingMatchedRecordsAndErrors(JobExecution jobExecution, String jobId) {
    String downloadErrorLink = bulkEditProcessingErrorsService.saveErrorFileAndGetDownloadLink(jobId, jobExecution);
    jobExecution.getExecutionContext().putString(OUTPUT_FILES_IN_STORAGE, saveResult(jobExecution, false) + PATHS_DELIMITER + (isNull(downloadErrorLink) ? EMPTY : downloadErrorLink) + PATHS_DELIMITER + saveJsonResult(jobExecution, true));
  }

  private void handleProcessingMarcFile(JobExecution jobExecution) {
    jobExecution.getExecutionContext().putString(OUTPUT_FILES_IN_STORAGE, jobExecution.getExecutionContext().getString(OUTPUT_FILES_IN_STORAGE) + PATHS_DELIMITER + saveMarcResult(jobExecution, false));
  }

  private void processJobAfter(String jobId, JobParameters jobParameters) {
    var tempOutputFilePath = jobParameters.getString(TEMP_OUTPUT_FILE_PATH);
    if (StringUtils.isBlank(tempOutputFilePath) ||
      jobParameters.getParameters().containsKey(EXPORT_TYPE) && (BULK_EDIT_UPDATE.getValue().equals(jobParameters.getString(EXPORT_TYPE)))) {
      return;
    }
    String path = FilenameUtils.getFullPath(tempOutputFilePath);
    String fileNameStart = FilenameUtils.getName(tempOutputFilePath);
    if (StringUtils.isBlank(path) || StringUtils.isBlank(fileNameStart)) {
      return;
    }
    var files = localFilesStorage.walk(path)
      .filter(name -> FilenameUtils.getName(name).startsWith(fileNameStart)).collect(Collectors.toList());
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

  private boolean isBulkEditIdentifiersJob(JobExecution jobExecution) {
    return jobExecution.getJobInstance().getJobName().contains(BULK_EDIT_IDENTIFIERS.getValue());
  }

  private boolean isBulkEditQueryJob(JobExecution jobExecution) {
    return jobExecution.getJobInstance().getJobName().contains(BULK_EDIT_QUERY.getValue());
  }

  private boolean isBulkEditJob(JobExecution jobExecution) {
    return isBulkEditIdentifiersJob(jobExecution) || isBulkEditQueryJob(jobExecution);
  }

  private boolean isBursarFeesFinesJob(JobExecution jobExecution) {
    return jobExecution.getJobInstance().getJobName().contains(BURSAR_FEES_FINES.getValue());
  }

  private String saveResult(JobExecution jobExecution, boolean isSourceShouldBeDeleted) {
    var path = preparePath(jobExecution);
    try {
      if (isEmpty(path) || noRecordsFound(path)) {
        return EMPTY; // To prevent downloading empty file.
      }
      if (localFilesStorage.notExists(path) && remoteFilesStorage.containsFile(path)) {
        return remoteFilesStorage.objectToPresignedObjectUrl(path);
      }
      var obj = prepareObject(jobExecution, path);
      if (isBulkEditJob(jobExecution) || isBursarFeesFinesJob(jobExecution)) {
        obj = validatePath(obj);
      }
      jobExecution.getExecutionContext().putString(PATH_TO_MATCHED_RECORDS, obj);
      return remoteFilesStorage.objectToPresignedObjectUrl(
        remoteFilesStorage.uploadObject(obj, path, prepareDownloadFilename(jobExecution, path), "text/csv", isSourceShouldBeDeleted));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private String saveJsonResult(JobExecution jobExecution, boolean isSourceToBeDeleted) {
    var path = preparePath(jobExecution) +".json";
    try {
      if (isEmpty(path) || noRecordsFound(path)) {
        return EMPTY; // To prevent downloading empty file.
      }
      if (localFilesStorage.notExists(path) && remoteFilesStorage.containsFile(path)) {
        return remoteFilesStorage.objectToPresignedObjectUrl(path);
      }
      return remoteFilesStorage.objectToPresignedObjectUrl(
        remoteFilesStorage.uploadObject(prepareJsonObject(jobExecution, path), path, prepareDownloadJsonFilename(jobExecution, path), "text/csv", isSourceToBeDeleted));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private String saveMarcResult(JobExecution jobExecution, boolean isSourceToBeDeleted) {
    var path = jobExecution.getJobParameters().getString(TEMP_OUTPUT_MARC_PATH) + ".mrc";
    try {
      if (localFilesStorage.notExists(path)) {
        log.info("No MARC records found.");
        return EMPTY; // To prevent downloading empty file.
      }
      return remoteFilesStorage.objectToPresignedObjectUrl(
        remoteFilesStorage.uploadObject(prepareMrcObject(jobExecution, path), path, null, "text/plain", isSourceToBeDeleted));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private String preparePath(JobExecution jobExecution) {
    return jobExecution.getJobParameters().getString(TEMP_OUTPUT_FILE_PATH);
  }

  private boolean noRecordsFound(String path) throws Exception {
    if (localFilesStorage.notExists(path) && !remoteFilesStorage.containsFile(path)) {
      log.error("Path to found records does not exist: {}", path);
      return true;
    }

    if (localFilesStorage.notExists(path)) {
      return CsvHelper.readRecordsFromStorage(remoteFilesStorage, path, UserFormat.class, true).isEmpty();
    }

    try (var lines = localFilesStorage.lines(path)) {
      return lines.count() <= (path.endsWith(".json") ? 0 : 1);
    }

  }

  private String prepareObject(JobExecution jobExecution, String path) {
    return jobExecution.getJobParameters().getString(JobParameterNames.JOB_ID) + PATH_SEPARATOR + FilenameUtils.getName(path) + CSV_EXTENSION;
  }

  private String prepareJsonObject(JobExecution jobExecution, String path) {
    return jobExecution.getJobParameters().getString(JobParameterNames.JOB_ID) + PATH_SEPARATOR + FilenameUtils.getName(path) + ".json";
  }

  private String prepareMrcObject(JobExecution jobExecution, String path) {
    return jobExecution.getJobParameters().getString(JobParameterNames.JOB_ID) + PATH_SEPARATOR + FilenameUtils.getName(path);
  }

  private String prepareDownloadFilename(JobExecution jobExecution, String path) {
    if (isBulkEditIdentifiersJob(jobExecution)) {
      return null;
    }
    return jobExecution.getJobParameters().getString(JobParameterNames.JOB_ID) + PATH_SEPARATOR + FilenameUtils.getName(path)
      .replace(MATCHED_RECORDS, CHANGED_RECORDS)
      .replace(UPDATED_PREFIX, EMPTY)
      .replace(INITIAL_PREFIX, EMPTY);
  }

  private String prepareDownloadJsonFilename(JobExecution jobExecution, String path) {
    if (isBulkEditIdentifiersJob(jobExecution)) {
      return null;
    }
    return jobExecution.getJobParameters().getString(JobParameterNames.JOB_ID) + PATH_SEPARATOR + FilenameUtils.getName(path)
      .replace(MATCHED_RECORDS, CHANGED_RECORDS)
      .replace(UPDATED_PREFIX, EMPTY)
      .replace(INITIAL_PREFIX, EMPTY);
  }

}
