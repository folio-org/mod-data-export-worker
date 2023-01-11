package org.folio.dew.batch.bulkedit.jobs.processquery.users;

import static org.folio.dew.domain.dto.UserFormat.getUserColumnHeaders;
import static org.folio.dew.domain.dto.UserFormat.getUserFieldsArray;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.folio.dew.batch.AbstractStorageStreamWriter;
import org.folio.dew.batch.CsvAndJsonWriter;
import org.folio.dew.batch.AbstractStorageStreamAndJsonWriter;
import org.folio.dew.domain.dto.EntityType;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.batch.CsvFileAssembler;
import org.folio.dew.batch.CsvPartStepExecutionListener;
import org.folio.dew.batch.JobCompletionNotificationListener;
import org.folio.dew.batch.bulkedit.jobs.BulkEditUserProcessor;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.UserFormat;
import org.folio.dew.repository.RemoteFilesStorage;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;

@Configuration
@Log4j2
@RequiredArgsConstructor
public class BulkEditUserCqlJobConfig {
  private static final int POOL_SIZE = 10;

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;
  private final UserClient userClient;
  private final RemoteFilesStorage remoteFilesStorage;
  @Bean
  public Job bulkEditUserCqlJob(
      JobCompletionNotificationListener jobCompletionNotificationListener,
      Step bulkEditUserCqlStep,
      JobRepository jobRepository) {
    return jobBuilderFactory
        .get(ExportType.BULK_EDIT_QUERY + "-" + EntityType.USER)
        .repository(jobRepository)
        .incrementer(new RunIdIncrementer())
        .listener(jobCompletionNotificationListener)
        .flow(bulkEditUserCqlStep)
        .end()
        .build();
  }

  @Bean
  public Step bulkEditUserCqlStep(
      Step bulkEditUserCqlPartitionStep,
      BulkEditUserCqlPartitioner partitioner,
      TaskExecutor asyncTaskExecutor,
      CsvFileAssembler csvFileAssembler) {
    return stepBuilderFactory
        .get("bulkEditCqlChunkStep")
        .partitioner("bulkEditUserCqlPartitionStep", partitioner)
        .taskExecutor(asyncTaskExecutor)
        .step(bulkEditUserCqlPartitionStep)
        .aggregator(csvFileAssembler)
        .build();
  }

  @Bean
  public Step bulkEditUserCqlPartitionStep(
    BulkEditCqlUserReader bulkEditCqlUserReader,
    AbstractStorageStreamWriter<UserFormat, RemoteFilesStorage> userWriter,
    BulkEditUserProcessor processor,
    CsvPartStepExecutionListener csvPartStepExecutionListener
  ) {
    return stepBuilderFactory
      .get("bulkEditUserCqlPartitionStep")
      .<User, UserFormat>chunk(100)
      .reader(bulkEditCqlUserReader)
      .processor(processor)
      .writer(userWriter)
      .faultTolerant()
      .allowStartIfComplete(false)
      .throttleLimit(POOL_SIZE)
      .listener(csvPartStepExecutionListener)
      .build();
  }

  @Bean
  @StepScope
  public BulkEditUserCqlPartitioner bulkEditUserCqlPartitioner(
    @Value("#{jobParameters['offset']}") Long offset,
    @Value("#{jobParameters['limit']}") Long limit,
    @Value("#{jobParameters['tempOutputFilePath']}") String tempOutputFilePath,
    @Value("#{jobParameters['query']}") String query) {
    return new BulkEditUserCqlPartitioner(offset, limit, tempOutputFilePath, userClient, query);
  }

  @Bean
  @StepScope
  public BulkEditCqlUserReader bulkEditCqlUserReader(
    @Value("#{jobParameters['query']}") String query,
    @Value("#{stepExecutionContext['offset']}") Long offset,
    @Value("#{stepExecutionContext['limit']}") Long limit) {
    return new BulkEditCqlUserReader(userClient, query, offset, limit);
  }

  @Bean
  @StepScope
  public AbstractStorageStreamAndJsonWriter<User, UserFormat, RemoteFilesStorage> userWriter(
    @Value("#{stepExecutionContext['tempOutputFilePath']}") String tempOutputFilePath) {
    return new CsvAndJsonWriter<>(tempOutputFilePath, getUserColumnHeaders(), getUserFieldsArray(), (field, i) -> field, remoteFilesStorage);
  }
}
