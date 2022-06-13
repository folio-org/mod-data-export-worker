package org.folio.dew.batch.marc;

import lombok.extern.log4j.Log4j2;
import org.folio.dew.batch.CsvPartitioner;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

@Log4j2
public class DataExportCsvPartitioner extends CsvPartitioner {

  private final String fileName;

  public DataExportCsvPartitioner(Long offset, Long limit, String tempOutputFilePath, String fileName) {
    super(offset, limit, tempOutputFilePath);

    this.fileName = fileName;
  }

  @Override
  protected Long getLimit() {
    try (var lines = Files.lines(Path.of(new URI(fileName)))) {
      return lines.count();
    } catch (Exception e) {
      log.error("Error reading file {}, reason: {}", fileName, e.getMessage());
      return 0L;
    }
  }

}
