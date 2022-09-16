package org.folio.dew.batch.marc;

import lombok.extern.log4j.Log4j2;
import org.folio.dew.batch.AbstractStorageStreamWriter;
import org.folio.dew.repository.LocalFilesStorage;
import org.folio.dew.repository.S3CompatibleResource;
import org.marc4j.marc.Record;

@Log4j2
public class MarcWriter extends AbstractStorageStreamWriter<Record, LocalFilesStorage> {
  public MarcWriter(String tempOutputFilePath, LocalFilesStorage localFilesStorage) {
    super(tempOutputFilePath, localFilesStorage);
  }
}
