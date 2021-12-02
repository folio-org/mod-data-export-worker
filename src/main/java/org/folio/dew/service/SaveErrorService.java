package org.folio.dew.service;

import static java.util.Objects.isNull;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Log4j2
public class SaveErrorService {

  public static final DateTimeFormatter CSV_NAME_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final String STORAGE_TEMPLATE = "." + File.separator + "storage" + File.separator + "%s";
  private static final String CSV_FILE_TEMPLATE = STORAGE_TEMPLATE + File.separator + "%s";

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
}
