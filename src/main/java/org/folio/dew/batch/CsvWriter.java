package org.folio.dew.batch;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.core.io.FileSystemResource;

@Log4j2
public class CsvWriter<T> extends FlatFileItemWriter<T> {

  public interface FieldProcessor {
    Object process(Object field, int i);
  }

  public CsvWriter(String tempOutputFilePath, String[] extractedFieldNames, FieldProcessor fieldProcessor) {
    if (StringUtils.isBlank(tempOutputFilePath)) {
      throw new IllegalArgumentException("tempOutputFilePath is blank");
    }

    BeanWrapperFieldExtractor<T> fieldExtractor = new BeanWrapperFieldExtractor<>() {
      @Override
      public Object[] extract(T item) {
        Object[] result = super.extract(item);
        if (ArrayUtils.isEmpty(result)) {
          return result;
        }

        for (int i = 0; i < result.length; i++) {
          Object o = result[i];
          if (o == null) {
            continue;
          }

          if (fieldProcessor != null) {
            o = fieldProcessor.process(o, i);
          }

          String s = o.toString();
          if (s.contains(",") || s.contains("\n")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
          }

          result[i] = s;
        }

        return result;
      }
    };
    fieldExtractor.setNames(extractedFieldNames);

    DelimitedLineAggregator<T> lineAggregator = new DelimitedLineAggregator<>();
    lineAggregator.setDelimiter(",");
    lineAggregator.setFieldExtractor(fieldExtractor);
    setLineAggregator(lineAggregator);

    setAppendAllowed(true);

    setResource(new FileSystemResource(tempOutputFilePath));

    log.info("Creating file {}.", tempOutputFilePath);
  }

}
