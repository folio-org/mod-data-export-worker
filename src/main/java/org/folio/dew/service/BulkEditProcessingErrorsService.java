package org.folio.dew.service;

import io.minio.ObjectWriteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.repository.MinIOObjectStorageRepository;
import org.folio.tenant.domain.dto.Error;
import org.folio.tenant.domain.dto.Errors;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

@Service
@Log4j2
@RequiredArgsConstructor
public class BulkEditProcessingErrorsService {

  public static final DateTimeFormatter CSV_NAME_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
  public static final String STORAGE = "storage";

  private static final String STORAGE_TEMPLATE = "." + File.separator + STORAGE + File.separator + "%s";
  private static final String CSV_FILE_TEMPLATE = STORAGE_TEMPLATE + File.separator + "%s";
  private static final String CONTENT_TYPE = "text/csv";
  public static final String COMMA_SEPARATOR = ",";
  public static final String BULK_EDIT_ERROR_TYPE_NAME = "BULK_EDIT_ERROR";

  private final MinIOObjectStorageRepository minIOObjectStorageRepository;

  public void saveErrorInCSV(String jobId, String affectedIdentifier, Throwable reasonForError, String fileName) {
    if (isNull(jobId) || isNull(affectedIdentifier) || isNull(reasonForError) || isNull(fileName)) {
      log.error("Some of the parameters is null, jobId: {}, affectedIdentifier: {}, reasonForError: {}, fileName: {}", jobId, affectedIdentifier, reasonForError, fileName);
      return;
    }
    var csvFileName = getCsvFileName(jobId, fileName);
    var errorMessages = reasonForError.getMessage().split(COMMA_SEPARATOR);
    for (var errorMessage: errorMessages) {
      var errorLine = affectedIdentifier + COMMA_SEPARATOR + errorMessage + System.lineSeparator();
      var pathToStorage = getPathToStorage(jobId);
      var pathToCSVFile = getPathToCsvFile(jobId, csvFileName);
      try {
        Files.createDirectories(pathToStorage);
        Files.write(pathToCSVFile, errorLine.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
      } catch (IOException ioException) {
        log.error("Failed to save {} error file with job id {} cause {}", csvFileName, jobId, ioException);
      }
    }
  }

  public Errors readErrorsFromCSV(String jobId, String fileName, Integer limit) throws BulkEditException {

    var csvFileName = getCsvFileName(jobId, fileName);
    var pathToCSVFile = getPathToCsvFile(jobId, csvFileName);

    if (Files.exists(pathToCSVFile)) {
      try (var lines = Files.lines(pathToCSVFile)) {
        var errors = lines.limit(limit)
          .map(message -> new Error().message(message).type(BULK_EDIT_ERROR_TYPE_NAME))
          .collect(toList());
        log.debug("Errors file {} processing completed", csvFileName);
        return new Errors().errors(errors).totalRecords(errors.size());
      } catch (IOException e) {
        log.error("Failed to read {} errors file for job id {} cause {}", csvFileName, jobId, e);
        throw new BulkEditException(format("Failed to read %s errors file for job id %s", csvFileName, jobId));
      }
    } else {
      log.debug("Errors file {} doesn't exist - empty error list returned", csvFileName);
      return new Errors().errors(emptyList()).totalRecords(0);
    }
  }

  public void removeTemporaryErrorStorage(String jobId) {
    Path storage = Paths.get("." + File.separator + BulkEditProcessingErrorsService.STORAGE);
    if (Files.exists(storage)) {
      try (Stream<Path> stream = Files.walk(storage)) {
        stream.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        log.info("Deleted temporary error storage of job {}.", jobId);
      } catch (IOException e) {
        log.error("Error occurred while deleting temporary error storage", e);
      }
    }
  }

  public String saveErrorFileAndGetDownloadLink(String jobId) {
    var pathToStorage = getPathToStorage(jobId);
    if (Files.exists(pathToStorage)) {
      try (Stream<Path> stream = Files.list(pathToStorage)) {
        Optional<Path> csvErrorFile = stream.filter(Files::isRegularFile).findFirst();
        if (csvErrorFile.isPresent()) {
          var filename = csvErrorFile.get().toAbsolutePath().toString();
          var downloadFilename = FilenameUtils.getName(filename);
          return saveErrorFile(downloadFilename, filename);
        } else {
          log.error("Download link cannot be created because CSV error file cannot be found at {}", pathToStorage.toString());
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
      ObjectWriteResponse objectWriteResponse = minIOObjectStorageRepository.uploadObject(downloadFilename, filename, downloadFilename, CONTENT_TYPE, false);
      log.info("CSV error file {} was saved into S3 successfully", downloadFilename);
      return getDownloadLink(objectWriteResponse);
    } catch (Exception e) {
      log.error("Error occurred while saving error csv file into S3", e);
      throw new IllegalStateException(e);
    }
  }

  private String getDownloadLink(ObjectWriteResponse objectWriteResponse) {
    try {
      return minIOObjectStorageRepository.objectWriteResponseToPresignedObjectUrl(objectWriteResponse);
    } catch (Exception e) {
      log.error("Error occurred while getting the link to error CSV file from S3", e);
      throw new IllegalStateException(e);
    }
  }

  private Path getPathToStorage(String jobId) {
    return Paths.get(format(STORAGE_TEMPLATE, jobId));
  }

  private Path getPathToCsvFile(String jobId, String csvFileName) {
    return Paths.get(format(CSV_FILE_TEMPLATE, jobId, csvFileName));
  }

  private String getCsvFileName(String jobId, String fileName) {
    var pathToStorage = getPathToStorage(jobId);
    List<String> names = new ArrayList<>();

    if (Files.exists(pathToStorage)) {
      names = Arrays.stream(requireNonNull(pathToStorage.toFile().listFiles()))
        .map(File::getName).collect(toList());
    }
    if (names.isEmpty()) {
      return LocalDate.now().format(CSV_NAME_DATE_FORMAT) + "-Errors-" + fileName;
    } else {
      return  names.get(0);
    }
  }
}
