package org.folio.dew.batch.bulkedit.jobs.processquery;

import static org.folio.dew.domain.dto.UserFormat.getUserColumnHeaders;
import static org.folio.dew.domain.dto.UserFormat.getUserFieldsArray;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.des.domain.dto.ExportType;
import org.folio.dew.batch.CsvFileAssembler;
import org.folio.dew.batch.CsvPartStepExecutionListener;
import org.folio.dew.batch.CsvWriter;
import org.folio.dew.batch.JobCompletionNotificationListener;
import org.folio.dew.batch.bulkedit.jobs.BulkEditUserProcessor;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.UserFormat;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;

@Configuration
@Log4j2
@RequiredArgsConstructor
public class BulkEditCqlJobConfig {
  private static final int POOL_SIZE = 10;

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;
  private final UserClient userClient;

  @Bean
  public Job bulkEditCqlJob(
      JobCompletionNotificationListener jobCompletionNotificationListener,
      Step bulkEditCqlStep,
      JobRepository jobRepository) {
    return jobBuilderFactory
        .get(ExportType.BULK_EDIT_QUERY.toString())
        .repository(jobRepository)
        .incrementer(new RunIdIncrementer())
        .listener(jobCompletionNotificationListener)
        .flow(bulkEditCqlStep)
        .end()
        .build();
  }

  @Bean
  public Step bulkEditCqlStep(
      Step bulkEditCqlPartitionStep,
      BulkEditCqlPartitioner partitioner,
      TaskExecutor asyncTaskExecutor,
      CsvFileAssembler csvFileAssembler) {
    return stepBuilderFactory
        .get("bulkEditCqlChunkStep")
        .partitioner("bulkEditCqlPartitionStep", partitioner)
        .taskExecutor(asyncTaskExecutor)
        .step(bulkEditCqlPartitionStep)
        .aggregator(csvFileAssembler)
        .build();
  }

  @Bean
  public Step bulkEditCqlPartitionStep(
    BulkEditCqlItemReader bulkEditCqlItemReader,
    FlatFileItemWriter<UserFormat> writer,
    BulkEditUserProcessor processor,
    CsvPartStepExecutionListener csvPartStepExecutionListener
  ) {
    return stepBuilderFactory
      .get("bulkEditCqlPartitionStep")
      .<User, UserFormat>chunk(100)
      .reader(bulkEditCqlItemReader)
      .processor(processor)
      .writer(writer)
      .faultTolerant()
      .allowStartIfComplete(false)
      .throttleLimit(POOL_SIZE)
      .listener(csvPartStepExecutionListener)
      .build();
  }

  @Bean
  @StepScope
  public BulkEditCqlPartitioner bulkEditCqlPartitioner(
    @Value("#{jobParameters['offset']}") Long offset,
    @Value("#{jobParameters['limit']}") Long limit,
    @Value("#{jobParameters['tempOutputFilePath']}") String tempOutputFilePath,
    @Value("#{jobParameters['query']}") String query) {
    return new BulkEditCqlPartitioner(offset, limit, tempOutputFilePath, userClient, query);
  }

  @Bean
  @StepScope
  public BulkEditCqlItemReader bulkEditCqlItemReader(
    @Value("#{jobParameters['query']}") String query,
    @Value("#{stepExecutionContext['offset']}") Long offset,
    @Value("#{stepExecutionContext['limit']}") Long limit) {
    return new BulkEditCqlItemReader(userClient, query, offset, limit);
  }

  @Bean
  @StepScope
  public FlatFileItemWriter<UserFormat> writer(
    @Value("#{stepExecutionContext['tempOutputFilePath']}") String tempOutputFilePath,
    @Value("#{stepExecutionContext['partition']}") Long partition) {
    return new CsvWriter<>(tempOutputFilePath, partition, getUserColumnHeaders(), getUserFieldsArray(), (field, i) -> field);
  }
}
