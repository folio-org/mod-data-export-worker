package org.folio.dew.batch.bulkedit.jobs;

import org.folio.dew.domain.dto.UserFormat;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.LineTokenizer;

public class JobConfigReaderHelper {

  public static LineMapper<UserFormat> createUserLineMapper() {
    DefaultLineMapper<UserFormat> userLineMapper = new DefaultLineMapper<>();

    LineTokenizer userLineTokenizer = createUserLineTokenizer();
    userLineMapper.setLineTokenizer(userLineTokenizer);

    FieldSetMapper<UserFormat> userInformationMapper =
      createUserInformationMapper();
    userLineMapper.setFieldSetMapper(userInformationMapper);

    return userLineMapper;
  }

  private static LineTokenizer createUserLineTokenizer() {
    DelimitedLineTokenizer userLineTokenizer = new DelimitedLineTokenizer();
    userLineTokenizer.setDelimiter(",");
    userLineTokenizer.setNames(UserFormat.getUserFieldsArray());
    return userLineTokenizer;
  }

  private static FieldSetMapper<UserFormat> createUserInformationMapper() {
    BeanWrapperFieldSetMapper<UserFormat> userInformationMapper =
      new BeanWrapperFieldSetMapper<>();
    userInformationMapper.setTargetType(UserFormat.class);
    return userInformationMapper;
  }
}
