package org.folio.dew.batch.marc;

import lombok.extern.log4j.Log4j2;
import org.folio.dew.batch.AbstractStorageStreamCsvWriter;
import org.folio.dew.repository.LocalFilesStorage;
import org.marc4j.marc.Record;

@Log4j2
public class MarcWriter extends AbstractStorageStreamCsvWriter<Record, LocalFilesStorage> {
  public MarcWriter(String tempOutputFilePath, LocalFilesStorage localFilesStorage) {
    super(tempOutputFilePath, localFilesStorage);
  }
}
