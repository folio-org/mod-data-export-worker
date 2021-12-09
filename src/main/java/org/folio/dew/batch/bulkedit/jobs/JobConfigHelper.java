package org.folio.dew.batch.bulkedit.jobs;

import org.folio.dew.domain.dto.UserFormat;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.LineTokenizer;

public class JobConfigHelper {


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
    userLineTokenizer.setNames(new String[]{
      "username",
      "id",
      "externalSystemId",
      "barcode",
      "active",
      "type",
      "patronGroup",
      "departments",
      "proxyFor",
      "lastName",
      "firstName",
      "middleName",
      "preferredFirstName",
      "email",
      "phone",
      "mobilePhone",
      "dateOfBirth",
      "addresses",
      "preferredContactTypeId",
      "enrollmentDate",
      "expirationDate",
      "createdDate",
      "updatedDate",
      "tags",
      "customFields"
    });
    return userLineTokenizer;
  }

  private static FieldSetMapper<UserFormat> createUserInformationMapper() {
    BeanWrapperFieldSetMapper<UserFormat> userInformationMapper =
      new BeanWrapperFieldSetMapper<>();
    userInformationMapper.setTargetType(UserFormat.class);
    return userInformationMapper;
  }


}
