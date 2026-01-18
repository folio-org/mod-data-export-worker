package org.folio.dew.batch;

import org.folio.dew.domain.dto.Formatable;
import org.folio.dew.repository.S3CompatibleResource;
import org.folio.dew.repository.S3CompatibleStorage;
import org.springframework.batch.item.Chunk;

import java.util.List;

public class CsvAndJsonListWriter<O, T extends Formatable<O>, R extends S3CompatibleStorage> extends AbstractStorageStreamWriter<List<T>, R> {
  private final CsvAndJsonWriter<O, T, R> delegate;

  public CsvAndJsonListWriter(String tempOutputFilePath, String columnHeaders, String[] extractedFieldNames, FieldProcessor fieldProcessor, R storage) {
    super(tempOutputFilePath, columnHeaders, extractedFieldNames, fieldProcessor, storage);
    delegate = new CsvAndJsonWriter<>(tempOutputFilePath, columnHeaders, extractedFieldNames, fieldProcessor, storage);
    setResource(new S3CompatibleResource<>(tempOutputFilePath, storage));
  }

  @Override
  public void write(Chunk<? extends List<T>> lists) throws Exception {
    var chunk = new Chunk<>(lists.getItems().stream().flatMap(List::stream).toList());
    delegate.write(chunk);
  }
}
