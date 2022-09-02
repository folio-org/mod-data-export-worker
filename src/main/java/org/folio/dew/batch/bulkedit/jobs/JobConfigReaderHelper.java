package org.folio.dew.batch.bulkedit.jobs;

import lombok.experimental.UtilityClass;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.LineTokenizer;

@UtilityClass
public class JobConfigReaderHelper {
  public static <T> LineMapper<T> createLineMapper(Class<T> clazz, String[] fields) {
    var lineMapper = new DefaultLineMapper<T>();
    lineMapper.setLineTokenizer(createLineTokenizer(fields));
    lineMapper.setFieldSetMapper(createInformationMapper(clazz));
    return lineMapper;
  }
  private static LineTokenizer createLineTokenizer(String[] fields) {
    var lineTokenizer = new DelimitedLineTokenizer();
    lineTokenizer.setDelimiter(",");
    lineTokenizer.setNames(fields);
    return lineTokenizer;
  }

  private static <T> FieldSetMapper<T> createInformationMapper(Class<T> clazz) {
    BeanWrapperFieldSetMapper<T> informationMapper = new BeanWrapperFieldSetMapper<>();
    informationMapper.setTargetType(clazz);
    return informationMapper;
  }
}
