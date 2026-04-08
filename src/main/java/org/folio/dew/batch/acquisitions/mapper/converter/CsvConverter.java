package org.folio.dew.batch.acquisitions.mapper.converter;

import java.util.Arrays;

public class CsvConverter<T> extends AbstractCsvConverter<T> {

  private final ExtractableField<T, String>[] fields;

  public CsvConverter(ExtractableField<T, String>[] fields) {
    this.fields = fields;
  }

  @Override
  protected ExtractableField<T, String>[] getFields() {
    return fields;
  }

  @Override
  protected String[] getHeaders() {
    return Arrays.stream(fields)
      .map(ExtractableField::getName)
      .toArray(String[]::new);
  }

}