package org.folio.dew.batch;

import static java.util.Objects.nonNull;

import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.Formatable;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.Assert;

import java.util.List;

@Log4j2
public class CsvListFileWriter<T, U extends Formatable<T>> extends FlatFileItemWriter<List<U>> {
  private CsvFileWriter<T, U> delegate;

  public CsvListFileWriter(String tempOutputFilePath, String columnHeaders, String[] extractedFieldNames, FieldProcessor fieldProcessor) {
    delegate = new CsvFileWriter<>(tempOutputFilePath, columnHeaders, extractedFieldNames, fieldProcessor);
    setResource(new FileSystemResource(tempOutputFilePath));
  }

  @Override
  public void write(Chunk<? extends List<U>> items) throws Exception {
    delegate.write(new Chunk<>(items.getItems().stream().flatMap(List::stream).toList()));
  }

  @Override
  public void afterPropertiesSet() {
    Assert.notNull(delegate, "Delegate was not set");
  }

  @Override
  public void open(ExecutionContext executionContext) {
    if (nonNull(delegate)) {
      ((ItemStream) delegate).open(executionContext);
    }
  }

  @Override
  public void update(ExecutionContext executionContext) {
    if (nonNull(delegate)) {
      ((ItemStream) delegate).update(executionContext);
    }
  }

  @Override
  public void close() {
    if (nonNull(delegate)) {
      ((ItemStream) delegate).close();
    }
  }
}
