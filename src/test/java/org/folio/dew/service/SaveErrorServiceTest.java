package org.folio.dew.service;

import org.folio.dew.error.BulkEditException;
import static org.folio.dew.service.SaveErrorService.CSV_NAME_DATE_FORMAT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

class SaveErrorServiceTest {

  @Test
  @DisplayName("Show that error file is created")
  void saveErrorInCSVTestSuccessTest() throws IOException {
    var jobId = UUID.randomUUID().toString();
    var affectedIdentifier = "ID";
    var reasonForError = new BulkEditException("Record not found");
    var identifiersFileName = "userUUIDs.csv";
    var csvFileName = LocalDate.now().format(CSV_NAME_DATE_FORMAT) + "-Errors-" + identifiersFileName;
    var pathToCsvFile = Paths.get("." + File.separator + "storage" + File.separator + jobId + File.separator + csvFileName);
    SaveErrorService saveErrorService = new SaveErrorService();
    saveErrorService.saveErrorInCSV(jobId, affectedIdentifier, reasonForError, identifiersFileName);
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
    var identifiersFileName = "userUUIDs.csv";
    var csvFileName = LocalDate.now().format(CSV_NAME_DATE_FORMAT) + "-Errors-" + identifiersFileName;
    var pathToCsvFile = Paths.get("." + File.separator + "storage" + File.separator + jobId + File.separator + csvFileName);
    SaveErrorService saveErrorService = new SaveErrorService();
    saveErrorService.saveErrorInCSV(null, affectedIdentifier, reasonForError, identifiersFileName);
    assertFalse(Files.exists(pathToCsvFile));
    saveErrorService.saveErrorInCSV(jobId, null, reasonForError, identifiersFileName);
    assertFalse(Files.exists(pathToCsvFile));
    saveErrorService.saveErrorInCSV(jobId, affectedIdentifier, null, identifiersFileName);
    assertFalse(Files.exists(pathToCsvFile));
    saveErrorService.saveErrorInCSV(jobId, affectedIdentifier, reasonForError, null);
    assertFalse(Files.exists(pathToCsvFile));
  }

  private void removeStorage() throws IOException {
    Files.walk(Paths.get("." + File.separator + "storage")).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
  }

}
