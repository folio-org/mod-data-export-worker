package org.folio.dew.batch.bulkedit.jobs.updatejob;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.folio.dew.domain.dto.EntityType.USER;
import static org.folio.dew.domain.dto.ExportType.BULK_EDIT_UPDATE;
import static org.folio.dew.domain.dto.JobParameterNames.UPDATED_FILE_NAME;
import static org.folio.dew.utils.Constants.FILE_NAME;
import static org.folio.dew.utils.Constants.JOB_NAME_POSTFIX_SEPARATOR;

import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import org.folio.dew.batch.JobCompletionNotificationListener;
import org.folio.dew.batch.bulkedit.jobs.JobConfigReaderHelper;
import org.folio.dew.batch.bulkedit.jobs.updatejob.listeners.BulkEditUpdateUserRecordsListener;
import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.UserFormat;
import org.folio.dew.repository.MinIOObjectStorageRepository;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Configuration
public class BulkEditUpdateUserRecordsJobConfig {

  @Bean
  public Job bulkEditUpdateUserRecordsJob(
    Step bulkEditUpdateUserRecordsStep,
    JobBuilderFactory jobBuilderFactory,
    BulkEditUpdateUserRecordsListener updateUserRecordsListener,
    JobCompletionNotificationListener completionListener) {
    return jobBuilderFactory
      .get(BULK_EDIT_UPDATE.getValue() + JOB_NAME_POSTFIX_SEPARATOR + USER.getValue())
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
    StepBuilderFactory stepBuilderFactory) {
    return stepBuilderFactory
      .get("bulkEditUpdateRecordsStep")
      .<UserFormat, User>chunk(10)
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
    MinIOObjectStorageRepository repository)
    throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException,
    InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
    LineMapper<UserFormat> userLineMapper = JobConfigReaderHelper.createUserLineMapper();
    return new FlatFileItemReaderBuilder<UserFormat>()
      .name("userReader")
      .resource(isEmpty(updatedFileName) ? new FileSystemResource(fileName) : new InputStreamResource(repository.getObject(updatedFileName)))
      .linesToSkip(1)
      .lineMapper(userLineMapper)
      .build();
  }
}
