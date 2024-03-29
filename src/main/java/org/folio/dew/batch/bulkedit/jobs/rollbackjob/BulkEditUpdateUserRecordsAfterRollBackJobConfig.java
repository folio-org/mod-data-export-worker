package org.folio.dew.batch.bulkedit.jobs.rollbackjob;

import org.folio.dew.batch.bulkedit.jobs.JobConfigReaderHelper;
import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.UserFormat;
import org.folio.dew.repository.LocalFilesStorage;
import org.folio.dew.repository.S3CompatibleResource;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BulkEditUpdateUserRecordsAfterRollBackJobConfig {

  @Bean
  public Job bulkEditRollBackJob(
    BulkEditUpdateUserRecordsAfterRollBackListener listener,
    Step bulkEditRollBackRecordsStep,
    JobRepository jobRepository) {
    return new JobBuilder("BULK_EDIT_ROLL_BACK", jobRepository)
      .incrementer(new RunIdIncrementer())
      .listener(listener)
      .flow(bulkEditRollBackRecordsStep)
      .end()
      .build();
  }

  @Bean
  public Step bulkEditRollBackRecordsStep(
    @Qualifier("bulkEditRollBackReader")
    ItemReader<UserFormat> reader,
    @Qualifier("bulkEditFilterUserRecordsForRollBackProcessor")
    ItemProcessor<UserFormat, User> processor,
    @Qualifier("bulkEditUpdateUserRecordsForRollBackWriter")
    ItemWriter<User> writer,
    JobRepository jobRepository,
    PlatformTransactionManager transactionManager) {
    return new StepBuilder("bulkEditRollBackRecordsStep", jobRepository)
      .<UserFormat, User>chunk(1, transactionManager)
      .reader(reader)
      .processor(processor)
      .writer(writer)
      .build();
  }

  @Bean
  @StepScope
  public FlatFileItemReader<UserFormat> bulkEditRollBackReader(@Value("#{jobParameters['fileName']}") String fileName, LocalFilesStorage localFilesStorage) {
   var userLineMapper = JobConfigReaderHelper.createLineMapper(UserFormat.class, UserFormat.getUserFieldsArray());
    return new FlatFileItemReaderBuilder<UserFormat>()
      .name("bulkEditRollBackReader")
      .resource(new S3CompatibleResource<>(fileName, localFilesStorage))
      .linesToSkip(1)
      .lineMapper(userLineMapper)
      .build();
  }
}
