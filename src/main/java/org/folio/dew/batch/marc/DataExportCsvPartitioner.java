package org.folio.dew.batch.marc;

import org.folio.dew.batch.CsvPartitioner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DataExportCsvPartitioner extends CsvPartitioner {

  private final String fileName;

  public DataExportCsvPartitioner(Long offset, Long limit, String tempOutputFilePath, String fileName) {
    super(offset, limit, tempOutputFilePath);

    this.fileName = fileName;
  }

  @Override
  protected Long getLimit() {
    try (var lines = Files.lines(Path.of(fileName))) {
      return lines.count();
    } catch (IOException e) {
      // TODO log file operation error
      return 0L;
    }
  }

}
