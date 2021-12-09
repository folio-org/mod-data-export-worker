package org.folio.dew.batch.bulkedit.jobs.rollbackjob;

import org.folio.dew.batch.bulkedit.jobs.JobConfigHelper;
import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.UserFormat;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.FileSystemResource;

public class BulkEditUpdateUserRecordsAfterRollBackJobConfig {

  @Bean
  public Job bulkEditRollBackJob(
    BulkEditUpdateUserRecordsAfterRollBackListener listener,
    Step bulkEditRollBackRecordsStep,
    JobBuilderFactory jobBuilderFactory) {
    return jobBuilderFactory
      .get("bulk-edit-roll-back")
      .incrementer(new RunIdIncrementer())
      .listener(listener)
      .flow(bulkEditRollBackRecordsStep)
      .end()
      .build();
  }

  @Bean
  public Step bulkEditRollBackRecordsStep(
    ItemReader<UserFormat> reader,
    ItemProcessor<UserFormat, User> processor,
    ItemWriter<User> writer,
    StepBuilderFactory stepBuilderFactory) {
    return stepBuilderFactory
      .get("bulkEditRollBackRecordsStep")
      .<UserFormat, User>chunk(10)
      .reader(reader)
      .processor(processor)
      .writer(writer)
      .build();
  }

  @Bean
  @StepScope
  public ItemReader<UserFormat> reader(@Value("#{jobParameters['fileName']}") String fileName) {
   LineMapper<UserFormat> userLineMapper = JobConfigHelper.createUserLineMapper();
    return new FlatFileItemReaderBuilder<UserFormat>()
      .name("userReader")
      .resource(new FileSystemResource(fileName))
      .linesToSkip(1)
      .lineMapper(userLineMapper)
      .build();
  }
}
