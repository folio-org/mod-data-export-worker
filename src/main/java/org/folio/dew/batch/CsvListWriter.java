package org.folio.dew.batch;

import org.folio.dew.repository.S3CompatibleResource;
import org.folio.dew.repository.S3CompatibleStorage;

import java.util.List;
import java.util.stream.Collectors;

public class CsvListWriter<T, R extends S3CompatibleStorage> extends AbstractStorageStreamWriter<List<T>, R> {
  private CsvWriter<T, R> delegate;

  public CsvListWriter(String tempOutputFilePath, String columnHeaders, String[] extractedFieldNames, FieldProcessor fieldProcessor, R storage, boolean isJsonNeedToBeGenerated) {
    super(tempOutputFilePath, columnHeaders, extractedFieldNames, fieldProcessor, storage, isJsonNeedToBeGenerated);
    delegate = new CsvWriter<>(tempOutputFilePath, columnHeaders, extractedFieldNames, fieldProcessor, storage, isJsonNeedToBeGenerated);
    setResource(new S3CompatibleResource<>(tempOutputFilePath, storage));
  }

  @Override
  public void write(List<? extends List<T>> lists) throws Exception {
    delegate.write(lists.stream().flatMap(List::stream).collect(Collectors.toList()));
  }
}
