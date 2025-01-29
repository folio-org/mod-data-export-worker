package org.folio.dew.service;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;
import static org.folio.dew.utils.Constants.PATH_SEPARATOR;
import static org.folio.dew.utils.Constants.PATH_TO_ERRORS;
import static org.folio.dew.utils.SystemHelper.validatePath;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.dew.domain.dto.Error;
import org.folio.dew.domain.dto.ErrorType;
import org.folio.dew.domain.dto.Errors;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.error.FileOperationException;
import org.folio.dew.repository.LocalFilesStorage;
import org.folio.dew.repository.RemoteFilesStorage;
import org.springframework.batch.core.JobExecution;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Log4j2
@RequiredArgsConstructor
public class BulkEditProcessingErrorsService {

  public static final DateTimeFormatter CSV_NAME_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
  public static final String STORAGE = "storage";

  private static final String FAILED_TO_SAVE_ERROR_FILE_PLACEHOLDER = "Failed to save {} error file with job id {} cause {}";
  private static final String STORAGE_TEMPLATE = "E" + "/" + STORAGE + "/" + "%s";
  private static final String CSV_FILE_TEMPLATE = STORAGE_TEMPLATE + "/" + "%s";
  private static final String CONTENT_TYPE = "text/csv";
  private static final int IDX_ERROR_IDENTIFIER = 0;
  private static final int IDX_ERROR_MSG = 1;
  private static final int IDX_ERROR_TYPE = 2;
  public static final String COMMA_SEPARATOR = ",";
  public static final String BULK_EDIT_ERROR_TYPE_NAME = "ERROR";

  private final RemoteFilesStorage remoteFilesStorage;

  private final LocalFilesStorage localFilesStorage;

  public synchronized void saveErrorInCSV(String jobId, String affectedIdentifier, Throwable reasonForError, String fileName, ErrorType errorType) {
    if (isNull(jobId) || isNull(affectedIdentifier) || isNull(reasonForError) || isNull(fileName)) {
      log.error("Some of the parameters is null, jobId: {}, affectedIdentifier: {}, reasonForError: {}, fileName: {}", jobId, affectedIdentifier, reasonForError, fileName);
      return;
    }
    var csvFileName = getCsvFileName(jobId, fileName);
    var errorMessages = reasonForError.getMessage().split(COMMA_SEPARATOR);
    for (var errorMessage: errorMessages) {
      var errorLine = affectedIdentifier + COMMA_SEPARATOR + errorMessage + COMMA_SEPARATOR + errorType + System.lineSeparator();
      var pathToCSVFile = getPathToCsvFile(jobId, csvFileName);
      try {
        localFilesStorage.append(pathToCSVFile, errorLine.getBytes(StandardCharsets.UTF_8));
      } catch (IOException ioException) {
        log.error(FAILED_TO_SAVE_ERROR_FILE_PLACEHOLDER, csvFileName, jobId, ioException);
      }
    }
  }

  public synchronized void saveErrorInCSV(String jobId, String affectedIdentifier, BulkEditException reasonForError, String fileName) {
    saveErrorInCSV(jobId, affectedIdentifier, reasonForError, fileName, reasonForError.getErrorType());
  }

  public synchronized void saveErrorInCSV(String jobId, String affectedIdentifier, String errorMessage, String fileName, ErrorType errorType) {
    if (isNull(jobId) || isNull(affectedIdentifier) || isNull(errorMessage) || isNull(fileName)) {
      log.error("Some of the parameters is null, jobId: {}, affectedIdentifier: {}, reasonForError: {}, fileName: {}", jobId, affectedIdentifier, errorMessage, fileName);
      return;
    }
    var csvFileName = getCsvFileName(jobId, fileName);
    var errorLine = affectedIdentifier + COMMA_SEPARATOR + errorMessage + COMMA_SEPARATOR + errorType + System.lineSeparator();
    var pathToCSVFile = getPathToCsvFile(jobId, csvFileName);
    try {
      localFilesStorage.append(pathToCSVFile, errorLine.getBytes(StandardCharsets.UTF_8));
    } catch (IOException ioException) {
      log.error(FAILED_TO_SAVE_ERROR_FILE_PLACEHOLDER, csvFileName, jobId, ioException);
    }
  }

  public Errors readErrorsFromCSV(String jobId, String fileName, Integer limit) {

    var csvFileName = getCsvFileName(jobId, fileName);
    var pathToCSVFile = getPathToCsvFile(jobId, csvFileName);

    if (localFilesStorage.exists(pathToCSVFile)) {
      try (var lines = localFilesStorage.lines(pathToCSVFile)) {
        var errors = lines.limit(limit)
          .map(Pattern.compile(",")::split)
          .map(message -> new Error().message(message[IDX_ERROR_IDENTIFIER] + COMMA_SEPARATOR + message[IDX_ERROR_MSG])
            .type(ErrorType.fromValue(message[IDX_ERROR_TYPE])))
          .collect(toList());
        log.info("Errors file {} processing completed", csvFileName);
        return new Errors().errors(errors).totalRecords(errors.size());
      } catch (IOException e) {
        log.error("Failed to read {} errors file for job id {} cause {}", csvFileName, jobId, e);
        throw new FileOperationException(format("Failed to read %s errors file for job id %s", csvFileName, jobId));
      }
    } else {
      log.info("Errors file {} doesn't exist - empty error list returned", csvFileName);
      return new Errors().errors(emptyList()).totalRecords(0);
    }
  }

  public void removeTemporaryErrorStorage() {
    localFilesStorage.delete("E" + File.separator + BulkEditProcessingErrorsService.STORAGE);
  }

  public String saveErrorFileAndGetDownloadLink(String jobId, JobExecution jobExecution) {
    var pathToStorage = getPathToStorage(jobId);
    if (localFilesStorage.exists(pathToStorage)) {
      try (Stream<String> stream = localFilesStorage.walk(pathToStorage)) {
        Optional<String> csvErrorFile = stream.findFirst();
        if (csvErrorFile.isPresent()) {
          var filename = csvErrorFile.get();
          var downloadFilename = jobId + PATH_SEPARATOR + FilenameUtils.getName(filename);
          downloadFilename = validatePath(downloadFilename);
          jobExecution.getExecutionContext().putString(PATH_TO_ERRORS, downloadFilename);
          return saveErrorFile(downloadFilename, filename);
        } else {
          log.error("Download link cannot be created because CSV error file cannot be found at {}", pathToStorage);
          throw new FileNotFoundException();
        }
      } catch (IOException e) {
        log.error("Error occurred while getting the error csv file from storage", e);
        throw new IllegalStateException(e);
      }
    } else {
      return null;
    }
  }

  private String saveErrorFile(String downloadFilename, String filename) {
    try {
      var objectWriteResponse = remoteFilesStorage.uploadObject(downloadFilename, filename, downloadFilename, CONTENT_TYPE, false);
      log.info("CSV error file {} was saved into S3 successfully", downloadFilename);
      return getDownloadLink(objectWriteResponse);
    } catch (Exception e) {
      log.error("Error occurred while saving error csv file into S3", e);
      throw new IllegalStateException(e);
    }
  }

  private String getDownloadLink(String objectWriteResponse) {
    try {
      return remoteFilesStorage.objectToPresignedObjectUrl(objectWriteResponse);
    } catch (Exception e) {
      log.error("Error occurred while getting the link to error CSV file from S3", e);
      throw new IllegalStateException(e);
    }
  }

  private String getPathToStorage(String jobId) {
    return format(STORAGE_TEMPLATE, jobId);
  }

  public String getPathToCsvFile(String jobId, String csvFileName) {
    return format(CSV_FILE_TEMPLATE, jobId, csvFileName);
  }

  public String getCsvFileName(String jobId, String fileName) {
    var pathToStorage = getPathToStorage(jobId);
    List<String> names = new ArrayList<>();

    if (localFilesStorage.exists(pathToStorage)) {
      names = localFilesStorage.walk(pathToStorage).map(x -> {
        var n = x.split("/");
        return n[n.length - 1];
      }).collect(Collectors.toList());
    }
    if (names.isEmpty()) {
      return LocalDate.now().format(CSV_NAME_DATE_FORMAT) + "-Matching-Records-Errors-" + fileName;
    } else {
      return  names.get(0);
    }
  }
}
