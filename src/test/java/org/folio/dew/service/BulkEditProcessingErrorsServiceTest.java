package org.folio.dew.service;

import org.folio.dew.BaseBatchTest;
import org.folio.dew.error.BulkEditException;
import static org.folio.dew.service.BulkEditProcessingErrorsService.CSV_NAME_DATE_FORMAT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

class BulkEditProcessingErrorsServiceTest extends BaseBatchTest {

  @Autowired
  private BulkEditProcessingErrorsService bulkEditProcessingErrorsService;

  @Test
  @DisplayName("Show that error file is created")
  void saveErrorInCSVTestSuccessTest() throws IOException {
    var jobId = UUID.randomUUID().toString();
    var affectedIdentifier = "ID";
    var reasonForError = new BulkEditException("Record not found");
    var fileName = "userUUIDs.csv";
    var csvFileName = LocalDate.now().format(CSV_NAME_DATE_FORMAT) + "-Errors-" + fileName;
    var pathToCsvFile = Paths.get("." + File.separator + BulkEditProcessingErrorsService.STORAGE + File.separator + jobId + File.separator + csvFileName);
    bulkEditProcessingErrorsService.saveErrorInCSV(jobId, affectedIdentifier, reasonForError, fileName);
    assertTrue(Files.exists(pathToCsvFile));
    List<String> lines = Files.readAllLines(pathToCsvFile);
    String expectedLine = affectedIdentifier + "," + reasonForError.getMessage();
    assertEquals(expectedLine, lines.get(0));
    removeStorage();
  }

  @Test
  @DisplayName("Show that error file is not created if at lease one of the parameter is null")
  void saveErrorInCSVTestFailedTest() {
    var jobId = UUID.randomUUID().toString();
    var affectedIdentifier = "ID";
    var reasonForError = new BulkEditException("Record not found");
    var fileName = "userUUIDs.csv";
    var csvFileName = LocalDate.now().format(CSV_NAME_DATE_FORMAT) + "-Errors-" + fileName;
    var pathToCsvFile = Paths.get("." + File.separator + BulkEditProcessingErrorsService.STORAGE + File.separator + jobId + File.separator + csvFileName);
    bulkEditProcessingErrorsService.saveErrorInCSV(null, affectedIdentifier, reasonForError, fileName);
    assertFalse(Files.exists(pathToCsvFile));
    bulkEditProcessingErrorsService.saveErrorInCSV(jobId, null, reasonForError, fileName);
    assertFalse(Files.exists(pathToCsvFile));
    bulkEditProcessingErrorsService.saveErrorInCSV(jobId, affectedIdentifier, null, fileName);
    assertFalse(Files.exists(pathToCsvFile));
    bulkEditProcessingErrorsService.saveErrorInCSV(jobId, affectedIdentifier, reasonForError, null);
    assertFalse(Files.exists(pathToCsvFile));
  }

  @Test
  @DisplayName("Read errors from csv file")
  void readErrorsFromCsvTest() throws BulkEditException, IOException {
    int numOfErrorLines = 3;
    int errorsPreviewLimit = 2;
    var jobId = UUID.randomUUID().toString();
    var reasonForError = new BulkEditException("Record not found");
    var fileName = "userUUIDs.csv";
    for (int i = 0; i < numOfErrorLines; i++) {
      bulkEditProcessingErrorsService.saveErrorInCSV(jobId, String.valueOf(i), reasonForError, fileName);
    }
    var errors = bulkEditProcessingErrorsService.readErrorsFromCSV(jobId, fileName, errorsPreviewLimit);
    assertThat(errors.getErrors(), Matchers.hasSize(errorsPreviewLimit));
    assertThat(errors.getTotalRecords(), Matchers.is(errorsPreviewLimit));
    removeStorage();
  }

  private void removeStorage() throws IOException {
    Files.walk(Paths.get("." + File.separator + BulkEditProcessingErrorsService.STORAGE)).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
  }

}
