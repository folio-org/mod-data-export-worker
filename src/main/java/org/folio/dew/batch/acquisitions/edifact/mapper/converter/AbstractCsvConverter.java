package org.folio.dew.batch.acquisitions.edifact.mapper.converter;

import static org.folio.dew.utils.Constants.COMMA;
import static org.folio.dew.utils.Constants.QUOTE;

import java.util.Arrays;

import org.folio.dew.utils.CsvHelper;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;

public abstract class AbstractCsvConverter<T> {

  private final DelimitedLineAggregator<String> lineAggregator;
  private final String delimiter;

  public AbstractCsvConverter() {
    this(COMMA);
  }

  public AbstractCsvConverter(String delimiter) {
    this.delimiter = delimiter;
    this.lineAggregator = new DelimitedLineAggregator<>();
    lineAggregator.setDelimiter(delimiter);
  }

  public String getCsvHeaders() {
    return lineAggregator.doAggregate(getHeaders());
  }

  public String convertEntryToCsv(T entry) {
    return lineAggregator.doAggregate(Arrays.stream(getFields())
        .map(field -> field.extract(entry))
        .map(field -> field == null ? "" : field)
        .map(field -> CsvHelper.escapeDelimiter(field, delimiter, QUOTE))
        .toArray());
  }

  protected abstract ExtractableField<T, String>[] getFields();

  protected abstract String[] getHeaders();

}