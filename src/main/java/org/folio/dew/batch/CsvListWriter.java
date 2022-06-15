package org.folio.dew.batch;

import static java.util.Objects.nonNull;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.Assert;

import java.util.List;
import java.util.stream.Collectors;

public class CsvListWriter<T> extends FlatFileItemWriter<List<T>> {
  private CsvWriter<T> delegate;

  public CsvListWriter(String tempOutputFilePath, String columnHeaders, String[] extractedFieldNames, CsvWriter.FieldProcessor fieldProcessor) {
    delegate = new CsvWriter<>(tempOutputFilePath, columnHeaders, extractedFieldNames, fieldProcessor);
    setResource(new FileSystemResource(tempOutputFilePath));
  }

  @Override
  public void write(List<? extends List<T>> lists) throws Exception {
    delegate.write(lists.stream().flatMap(List::stream).collect(Collectors.toList()));
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
