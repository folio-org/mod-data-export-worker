package org.folio.dew.batch;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.domain.dto.Formatable;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.core.io.FileSystemResource;

@Log4j2
public class CsvFileWriter<T, U extends Formatable<T>> extends FlatFileItemWriter<U> {
  public CsvFileWriter(String tempOutputFilePath, String columnHeaders, String[] extractedFieldNames, FieldProcessor fieldProcessor) {

    if (StringUtils.isBlank(tempOutputFilePath)) {
      throw new IllegalArgumentException("tempOutputFilePath is blank");
    }

    BeanWrapperFieldExtractor<U> fieldExtractor = new CsvFieldExtractor<>(fieldProcessor);

    fieldExtractor.setNames(extractedFieldNames);

    DelimitedLineAggregator<U> lineAggregator = new DelimitedLineAggregator<>();
    lineAggregator.setDelimiter(",");
    lineAggregator.setFieldExtractor(fieldExtractor);
    setLineAggregator(lineAggregator);

    setAppendAllowed(true);

    if (StringUtils.isNotBlank(columnHeaders)) {
      setHeaderCallback(writer -> writer.write(columnHeaders));
    }

    setResource(new FileSystemResource(tempOutputFilePath));

    log.info("Creating file {}.", tempOutputFilePath);
  }
}
