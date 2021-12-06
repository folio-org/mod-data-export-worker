package org.folio.dew.service;

import static java.util.Objects.isNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.dew.repository.MinIOObjectStorageRepository;
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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Log4j2
@RequiredArgsConstructor
public class SaveErrorService {

  public static final DateTimeFormatter CSV_NAME_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
  public static final String STORAGE = "storage";

  private static final String STORAGE_TEMPLATE = "." + File.separator + STORAGE + File.separator + "%s";
  private static final String CSV_FILE_TEMPLATE = STORAGE_TEMPLATE + File.separator + "%s";
  private static final String CONTENT_TYPE = "text/csv";

  private final MinIOObjectStorageRepository minIOObjectStorageRepository;

  public void saveErrorInCSV(String jobId, String affectedIdentifier, Throwable reasonForError, String identifiersFileName) {
    if (isNull(jobId) || isNull(affectedIdentifier) || isNull(reasonForError) || isNull(identifiersFileName)) {
      log.error("Some of the parameters is null, jobId: {}, affectedIdentifier: {}, reasonForError: {}, identifiersFileName: {}", jobId, affectedIdentifier, reasonForError, identifiersFileName);
      return;
    }
    var csvFileName = LocalDate.now().format(CSV_NAME_DATE_FORMAT) + "-Errors-" + identifiersFileName;
    var errorLine = affectedIdentifier + "," + reasonForError.getMessage() + System.lineSeparator();
    var pathToStorage = Paths.get(String.format(STORAGE_TEMPLATE, jobId));
    var pathToCSVFile = Paths.get(String.format(CSV_FILE_TEMPLATE, jobId, csvFileName));
    try {
      Files.createDirectories(pathToStorage);
      Files.write(pathToCSVFile, errorLine.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    } catch (IOException ioException) {
      log.error("Failed to save {} error file with job id {} cause {}", csvFileName, jobId, ioException);
    }
  }

  public void removeTemporaryErrorStorage(String jobId) {
    Path storage = Paths.get("." + File.separator + SaveErrorService.STORAGE);
    if (Files.exists(storage)) {
      try {
        Files.walk(storage)
          .sorted(Comparator.reverseOrder())
          .map(Path::toFile)
          .forEach(File::delete);
        log.info("Deleted temporary error storage of job {}.", jobId);
      } catch (IOException e) {
        log.error("Error occurred while deleting temporary error storage", e);
      }
    }
  }

  public String saveErrorFileAndGetDownloadLink(String jobId) {
    var pathToStorage = Paths.get(String.format(STORAGE_TEMPLATE, jobId));
    if (Files.exists(pathToStorage)) {
      try {
        Optional<Path> csvErrorFile = Files.list(pathToStorage).filter(Files::isRegularFile).findFirst();
        if (csvErrorFile.isPresent()) {
          var filename = csvErrorFile.get().toAbsolutePath().toString();
          var downloadFilename = FilenameUtils.getName(filename);
          try {
            minIOObjectStorageRepository.uploadObject(downloadFilename, filename, downloadFilename, CONTENT_TYPE);
            log.info("CSV error file {} was saved into S3 successfully", downloadFilename);
            return getDownloadLink(downloadFilename);
          } catch (Exception e) {
            log.error("Error occurred while saving error csv file into S3", e);
            throw new IllegalStateException(e);
          }
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

  private String getDownloadLink(String destObject) {
    try {
      return minIOObjectStorageRepository.objectWriteResponseToPresignedObjectUrl(
        minIOObjectStorageRepository.composeObject(destObject, List.of(destObject), destObject, CONTENT_TYPE)
      );
    } catch (Exception e) {
      log.error("Error occurred while getting the link to error CSV file from S3", e);
      throw new IllegalStateException(e);
    }
  }
}
