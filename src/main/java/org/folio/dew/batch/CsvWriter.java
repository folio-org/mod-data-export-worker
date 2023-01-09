package org.folio.dew.batch;

import lombok.extern.log4j.Log4j2;
import org.folio.dew.repository.S3CompatibleStorage;

@Log4j2
public class CsvWriter<T, R extends S3CompatibleStorage> extends AbstractStorageStreamWriter<T, R> {

  public CsvWriter(String tempOutputFilePath, String columnHeaders, String[] extractedFieldNames, FieldProcessor fieldProcessor, R storage, boolean isJsonNeedToBeGenerated) {
    super(tempOutputFilePath, columnHeaders, extractedFieldNames, fieldProcessor, storage, isJsonNeedToBeGenerated);
  }

}
