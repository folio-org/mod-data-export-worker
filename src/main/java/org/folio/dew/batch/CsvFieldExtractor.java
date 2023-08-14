package org.folio.dew.batch;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;

public class CsvFieldExtractor<T> extends BeanWrapperFieldExtractor<T> {
  private final FieldProcessor fieldProcessor;

  public CsvFieldExtractor(FieldProcessor fieldProcessor) {
    this.fieldProcessor = fieldProcessor;
  }

  @Override
  public Object[] extract(T item) {
    Object[] result = super.extract(item);
    if (ArrayUtils.isEmpty(result)) {
      return result;
    }

    for (var i = 0; i < result.length; i++) {
      Object o = result[i];
      if (o == null) {
        continue;
      }

      if (fieldProcessor != null) {
        o = fieldProcessor.process(o, i);
      }

      var s = o.toString();

      if (s.contains("\"")) {
        s = s.replace("\"", "\"\"");
      }

      if (s.contains(",") || s.contains("\n")) {
        s = "\"" + s + "\"";
      }

      result[i] = s;
    }

    return result;
  }
}
