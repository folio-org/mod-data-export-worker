package org.folio.dew.batch;

import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.Formatable;
import org.folio.dew.repository.S3CompatibleStorage;

@Log4j2
public class CsvAndJsonWriter<O, T extends Formatable<O>, R extends S3CompatibleStorage> extends AbstractStorageStreamCsvAndJsonWriter<O, T, R> {

  public CsvAndJsonWriter(String tempOutputFilePath, String columnHeaders, String[] extractedFieldNames, FieldProcessor fieldProcessor, R storage) {
    super(tempOutputFilePath, columnHeaders, extractedFieldNames, fieldProcessor, storage);
  }

}
