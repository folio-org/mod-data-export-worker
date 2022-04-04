package org.folio.dew.batch.bulkedit.jobs;

import lombok.experimental.UtilityClass;
import org.folio.dew.domain.dto.ItemFormat;
import org.folio.dew.domain.dto.UserFormat;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.LineTokenizer;

@UtilityClass
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

  public static LineMapper<ItemFormat> createItemLineMapper() {
    DefaultLineMapper<ItemFormat> itemLineMapper = new DefaultLineMapper<>();

    LineTokenizer itemLineTokenizer = createItemLineTokenizer();
    itemLineMapper.setLineTokenizer(itemLineTokenizer);

    FieldSetMapper<ItemFormat> itemInformationMapper =
      createItemInformationMapper();
    itemLineMapper.setFieldSetMapper(itemInformationMapper);

    return itemLineMapper;
  }

  private static LineTokenizer createItemLineTokenizer() {
    DelimitedLineTokenizer itemLineTokenizer = new DelimitedLineTokenizer();
    itemLineTokenizer.setDelimiter(",");
    itemLineTokenizer.setNames(ItemFormat.getItemFieldsArray());
    return itemLineTokenizer;
  }

  private static FieldSetMapper<ItemFormat> createItemInformationMapper() {
    BeanWrapperFieldSetMapper<ItemFormat> itemInformationMapper =
      new BeanWrapperFieldSetMapper<>();
    itemInformationMapper.setTargetType(ItemFormat.class);
    return itemInformationMapper;
  }
}
