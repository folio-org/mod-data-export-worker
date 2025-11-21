package org.folio.dew.batch;

import org.folio.dew.repository.AbstractFilesStorage;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class CsvWriter<T, R extends AbstractFilesStorage> extends AbstractStorageStreamWriter<T, R> {

  public CsvWriter(String tempOutputFilePath, String columnHeaders, String[] extractedFieldNames, FieldProcessor fieldProcessor, R storage) {
    super(tempOutputFilePath, columnHeaders, extractedFieldNames, fieldProcessor, storage);
  }

}
