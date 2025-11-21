package org.folio.dew.batch.marc;

import lombok.extern.log4j.Log4j2;
import org.folio.dew.batch.CsvPartitioner;
import org.folio.dew.repository.LocalFilesStorage;
import org.folio.s3.exception.S3ClientException;

import java.io.IOException;

@Log4j2
public class DataExportCsvPartitioner extends CsvPartitioner {

  private final String fileName;
  private final LocalFilesStorage localFilesStorage;

  public DataExportCsvPartitioner(Long offset, Long limit, String tempOutputFilePath, String fileName, LocalFilesStorage localFilesStorage) {
    super(offset, limit, tempOutputFilePath);

    this.fileName = fileName;
    this.localFilesStorage = localFilesStorage;
  }

  @Override
  protected Long getLimit() {
    try {
      return localFilesStorage.numLines(fileName);
    } catch (IOException | S3ClientException e) {
      log.error("Error reading file {}, reason: {}", fileName, e.getMessage());
      return 0L;
    }
  }

}
