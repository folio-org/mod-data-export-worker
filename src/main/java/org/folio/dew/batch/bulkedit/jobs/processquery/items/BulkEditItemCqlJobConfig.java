package org.folio.dew.batch.bulkedit.jobs.processquery.items;

import org.folio.dew.batch.AbstractStorageStreamWriter;
import org.folio.dew.batch.CsvAndJsonWriter;
import org.folio.dew.batch.CsvFileAssembler;
import org.folio.dew.batch.CsvPartStepExecutionListener;
import org.folio.dew.batch.JobCompletionNotificationListener;
import org.folio.dew.batch.bulkedit.jobs.BulkEditItemProcessor;
import org.folio.dew.batch.AbstractStorageStreamAndJsonWriter;
import org.folio.dew.client.InventoryClient;
import org.folio.dew.domain.dto.EntityType;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.Item;
import org.folio.dew.domain.dto.ItemFormat;
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

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import static org.folio.dew.domain.dto.ItemFormat.getItemColumnHeaders;
import static org.folio.dew.domain.dto.ItemFormat.getItemFieldsArray;

@Configuration
@Log4j2
@RequiredArgsConstructor
public class BulkEditItemCqlJobConfig {
  private static final int POOL_SIZE = 10;

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;
  private final InventoryClient inventoryClient;
  private final RemoteFilesStorage remoteFilesStorage;

  @Bean
  public Job bulkEditItemCqlJob(
      JobCompletionNotificationListener jobCompletionNotificationListener,
      Step bulkEditItemCqlStep,
      JobRepository jobRepository) {
    return jobBuilderFactory
        .get(ExportType.BULK_EDIT_QUERY + "-" + EntityType.ITEM)
        .repository(jobRepository)
        .incrementer(new RunIdIncrementer())
        .listener(jobCompletionNotificationListener)
        .flow(bulkEditItemCqlStep)
        .end()
        .build();
  }

  @Bean
  public Step bulkEditItemCqlStep(
      Step bulkEditItemCqlPartitionStep,
      BulkEditCqlItemPartitioner partitioner,
      TaskExecutor asyncTaskExecutor,
      CsvFileAssembler csvFileAssembler) {
    return stepBuilderFactory
        .get("bulkEditCqlChunkStep")
        .partitioner("bulkEditItemCqlPartitionStep", partitioner)
        .taskExecutor(asyncTaskExecutor)
        .step(bulkEditItemCqlPartitionStep)
        .aggregator(csvFileAssembler)
        .build();
  }

  @Bean
  public Step bulkEditItemCqlPartitionStep(
    BulkEditCqlItemReader bulkEditCqlItemReader,
    AbstractStorageStreamWriter<ItemFormat, RemoteFilesStorage> itemWriter,
    BulkEditItemProcessor processor,
    CsvPartStepExecutionListener csvPartStepExecutionListener
  ) {
    return stepBuilderFactory
      .get("bulkEditItemCqlPartitionStep")
      .<Item, ItemFormat>chunk(100)
      .reader(bulkEditCqlItemReader)
      .processor(processor)
      .writer(itemWriter)
      .faultTolerant()
      .allowStartIfComplete(false)
      .throttleLimit(POOL_SIZE)
      .listener(csvPartStepExecutionListener)
      .build();
  }

  @Bean
  @StepScope
  public BulkEditCqlItemPartitioner bulkEditItemCqlPartitioner(
    @Value("#{jobParameters['offset']}") Long offset,
    @Value("#{jobParameters['limit']}") Long limit,
    @Value("#{jobParameters['tempOutputFilePath']}") String tempOutputFilePath,
    @Value("#{jobParameters['query']}") String query) {
    return new BulkEditCqlItemPartitioner(offset, limit, tempOutputFilePath, inventoryClient, query);
  }

  @Bean
  @StepScope
  public BulkEditCqlItemReader bulkEditCqlItemReader(
    @Value("#{jobParameters['query']}") String query,
    @Value("#{stepExecutionContext['offset']}") Long offset,
    @Value("#{stepExecutionContext['limit']}") Long limit) {
    return new BulkEditCqlItemReader(inventoryClient, query, offset, limit);
  }

  @Bean
  @StepScope
  public AbstractStorageStreamAndJsonWriter<Item, ItemFormat, RemoteFilesStorage> itemWriter(
    @Value("#{stepExecutionContext['tempOutputFilePath']}") String tempOutputFilePath) {
    return new CsvAndJsonWriter<>(tempOutputFilePath, getItemColumnHeaders(), getItemFieldsArray(), (field, i) -> field, remoteFilesStorage);
  }
}
