package org.folio.dew.batch.acquisitions.edifact.mapper.converter;

import static java.util.Optional.ofNullable;
import static org.folio.dew.utils.Constants.COMMA;
import static org.folio.dew.utils.Constants.QUOTE;
import static org.folio.dew.utils.CsvHelper.escapeDelimiter;

import java.util.Arrays;

import org.springframework.batch.item.file.transform.DelimitedLineAggregator;

public abstract class AbstractCsvConverter<T> {

  private final DelimitedLineAggregator<String> lineAggregator;
  private final String delimiter;

  protected AbstractCsvConverter() {
    this(COMMA);
  }

  protected AbstractCsvConverter(String delimiter) {
    this.delimiter = delimiter;
    this.lineAggregator = new DelimitedLineAggregator<>();
    lineAggregator.setDelimiter(delimiter);
  }

  public String getCsvHeaders() {
    return lineAggregator.doAggregate(getHeaders());
  }

  public String convertEntryToCsv(T entry) {
    return lineAggregator.doAggregate(Arrays.stream(getFields())
        .map(field -> ofNullable(extractField(entry, field)).orElse(""))
        .map(field -> escapeDelimiter(field, delimiter, QUOTE))
        .toArray());
  }

  protected String extractField(T entry, ExtractableField<T, String> field) {
    return field.extract(entry);
  }

  protected abstract ExtractableField<T, String>[] getFields();

  protected abstract String[] getHeaders();

}
