package org.folio.dew.batch.bulkedit.jobs.updatejob;

import org.folio.dew.batch.JobCompletionNotificationListener;
import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.UserFormat;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import static org.folio.dew.domain.dto.EntityType.USER;
import static org.folio.dew.domain.dto.ExportType.BULK_EDIT_UPDATE;
import static org.folio.dew.utils.Constants.FILE_NAME;

@Configuration
public class BulkEditUpdateUserRecordsJobConfig {

  @Bean
  public Job bulkEditUpdateUserRecordsJob(
    Step bulkEditUpdateRecordsStep,
    JobBuilderFactory jobBuilderFactory,
    JobCompletionNotificationListener listener) {
    return jobBuilderFactory
      .get(BULK_EDIT_UPDATE.getValue() + "-" + USER.getValue())
      .incrementer(new RunIdIncrementer())
      .listener(listener)
      .flow(bulkEditUpdateRecordsStep)
      .end()
      .build();
  }

  @Bean
  public Step bulkEditUpdateRecordsStep(
    ItemReader<UserFormat> csvUserRecordsReader,
    ItemProcessor<UserFormat, User> processor,
    @Qualifier("updateUserRecordsWriter") ItemWriter<User> writer,
    @Qualifier("updateUserWriteListener") ItemWriteListener<User> updateUserWriteListener,
    StepBuilderFactory stepBuilderFactory) {
    return stepBuilderFactory
      .get("bulkEditUpdateRecordsStep")
      .<UserFormat, User>chunk(10)
      .reader(csvUserRecordsReader)
      .processor(processor)
      .writer(writer)
      .listener(updateUserWriteListener)
      .build();
  }

  @Bean
  @StepScope
  public FlatFileItemReader<UserFormat> csvUserRecordsReader(@Value("#{jobParameters['" + FILE_NAME + "']}") String fileName) {
    LineMapper<UserFormat> userLineMapper = createUserLineMapper();

    return new FlatFileItemReaderBuilder<UserFormat>()
      .name("userReader")
      .resource(new FileSystemResource(fileName))
      .linesToSkip(1)
      .lineMapper(userLineMapper)
      .build();
  }

  private LineMapper<UserFormat> createUserLineMapper() {
    DefaultLineMapper<UserFormat> userLineMapper = new DefaultLineMapper<>();

    LineTokenizer userLineTokenizer = createUserLineTokenizer();
    userLineMapper.setLineTokenizer(userLineTokenizer);

    FieldSetMapper<UserFormat> userInformationMapper =
      createUserInformationMapper();
    userLineMapper.setFieldSetMapper(userInformationMapper);

    return userLineMapper;
  }

  private LineTokenizer createUserLineTokenizer() {
    DelimitedLineTokenizer userLineTokenizer = new DelimitedLineTokenizer();
    userLineTokenizer.setDelimiter(",");
    userLineTokenizer.setNames(UserFormat.getUserFieldsArray());
    return userLineTokenizer;
  }

  private FieldSetMapper<UserFormat> createUserInformationMapper() {
    BeanWrapperFieldSetMapper<UserFormat> userInformationMapper =
      new BeanWrapperFieldSetMapper<>();
    userInformationMapper.setTargetType(UserFormat.class);
    return userInformationMapper;
  }


}
