package org.folio.dew.batch.bulkedit.jobs.updatejob;

import org.folio.dew.batch.JobCompletionNotificationListener;
import org.folio.dew.batch.bulkedit.jobs.JobConfigReaderHelper;
import org.folio.dew.batch.bulkedit.jobs.updatejob.listeners.BulkEditUpdateUserRecordsListener;
import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.UserFormat;
import org.folio.dew.repository.LocalFilesStorage;
import org.folio.dew.repository.RemoteFilesStorage;
import org.folio.dew.repository.S3CompatibleResource;
import org.springframework.batch.core.ItemWriteListener;
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
import org.springframework.core.io.InputStreamResource;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.folio.dew.domain.dto.EntityType.USER;
import static org.folio.dew.domain.dto.ExportType.BULK_EDIT_UPDATE;
import static org.folio.dew.domain.dto.JobParameterNames.UPDATED_FILE_NAME;
import static org.folio.dew.utils.Constants.FILE_NAME;
import static org.folio.dew.utils.Constants.JOB_NAME_POSTFIX_SEPARATOR;

@Configuration
public class BulkEditUpdateUserRecordsJobConfig {

  @Bean
  public Job bulkEditUpdateUserRecordsJob(
    Step bulkEditUpdateUserRecordsStep,
    JobRepository jobRepository,
    BulkEditUpdateUserRecordsListener updateUserRecordsListener,
    JobCompletionNotificationListener completionListener) {
    return new JobBuilder(BULK_EDIT_UPDATE.getValue() + JOB_NAME_POSTFIX_SEPARATOR + USER.getValue(), jobRepository)
      .incrementer(new RunIdIncrementer())
      .listener(updateUserRecordsListener)
      .listener(completionListener)
      .flow(bulkEditUpdateUserRecordsStep)
      .end()
      .build();
  }

  @Bean
  public Step bulkEditUpdateUserRecordsStep(
    ItemReader<UserFormat> csvUserRecordsReader,
    @Qualifier("bulkEditUpdateUserRecordsProcessor")
    ItemProcessor<UserFormat, User> processor,
    @Qualifier("updateUserRecordsWriter") ItemWriter<User> writer,
    @Qualifier("updateRecordWriteListener") ItemWriteListener<User> updateRecordWriteListener,
    JobRepository jobRepository, PlatformTransactionManager transactionManager) {
    return new StepBuilder("bulkEditUpdateRecordsStep", jobRepository)
      .<UserFormat, User>chunk(10, transactionManager)
      .reader(csvUserRecordsReader)
      .processor(processor)
      .writer(writer)
      .listener(updateRecordWriteListener)
      .build();
  }

  @Bean
  @StepScope
  public FlatFileItemReader<UserFormat> csvUserRecordsReader(
    @Value("#{jobParameters['" + FILE_NAME + "']}") String fileName,
    @Value("#{jobParameters['" + UPDATED_FILE_NAME + "']}") String updatedFileName,
    LocalFilesStorage localFilesStorage, RemoteFilesStorage remoteFilesStorage)
    throws IOException {
    var userLineMapper = JobConfigReaderHelper.createLineMapper(UserFormat.class, UserFormat.getUserFieldsArray());
    return new FlatFileItemReaderBuilder<UserFormat>()
      .name("userReader")
      .resource(isEmpty(updatedFileName) ? new S3CompatibleResource<>(fileName, localFilesStorage) : new InputStreamResource(remoteFilesStorage.newInputStream(updatedFileName)))
      .linesToSkip(1)
      .lineMapper(userLineMapper)
      .build();
  }
}
