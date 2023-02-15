package org.folio.dew.batch.marc;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.batch.AbstractStorageStreamWriter;
import org.folio.dew.batch.CsvFileAssembler;
import org.folio.dew.batch.CsvPartStepExecutionListener;
import org.folio.dew.batch.JobCompletionNotificationListener;
import org.folio.dew.domain.dto.ItemIdentifier;
import org.folio.dew.error.NonSupportedEntityException;
import org.folio.dew.repository.LocalFilesStorage;
import org.marc4j.marc.Record;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@Log4j2
@RequiredArgsConstructor
public class DataExportJobConfig {
  private static final int POOL_SIZE = 10;
  private final LocalFilesStorage localFilesStorage;

  @Bean
  public Job dataExportJob(
      JobCompletionNotificationListener jobCompletionNotificationListener,
      Step dataExportStep,
      JobRepository jobRepository) {
    return new JobBuilder("MARC_EXPORT", jobRepository) // TODO change to ExportType.MARC_EXPORT.toString() after schema update
        .repository(jobRepository)
        .incrementer(new RunIdIncrementer())
        .listener(jobCompletionNotificationListener)
        .flow(dataExportStep)
        .end()
        .build();
  }

  @Bean
  public Step dataExportStep(
      Step dataExportPartitionStep,
      DataExportCsvPartitioner partitioner,
      TaskExecutor asyncTaskExecutor,
      CsvFileAssembler csvFileAssembler,
      JobRepository jobRepository) {
    return new StepBuilder("dataExportChunkStep", jobRepository)
        .partitioner("dataExportPartitionStep", partitioner)
        .taskExecutor(asyncTaskExecutor)
        .step(dataExportPartitionStep)
        .aggregator(csvFileAssembler)
        .build();
  }

  @Bean
  public Step dataExportPartitionStep(
    DataExportCsvItemReader dataExportCsvItemReader,
    AbstractStorageStreamWriter<Record, LocalFilesStorage> recordWriter,
    ItemProcessor<ItemIdentifier, Record> processor,
    CsvPartStepExecutionListener csvPartStepExecutionListener,
    JobRepository jobRepository,
    PlatformTransactionManager transactionManager
  ) {
    return new StepBuilder("dataExportPartitionStep", jobRepository)
      .<ItemIdentifier, Record>chunk(100, transactionManager)
      .reader(dataExportCsvItemReader)
      .processor(processor)
      .writer(recordWriter)
      .faultTolerant()
      .allowStartIfComplete(false)
      .throttleLimit(POOL_SIZE)
      .listener(csvPartStepExecutionListener)
      .build();
  }

  @Bean
  @StepScope
  public DataExportCsvPartitioner dataExportPartitioner(
    @Value("#{jobParameters['fileName']}") String fileName,
    @Value("#{jobParameters['offset']}") Long offset,
    @Value("#{jobParameters['limit']}") Long limit,
    @Value("#{jobParameters['tempOutputFilePath']}") String tempOutputFilePath) {
    return new DataExportCsvPartitioner(offset, limit, tempOutputFilePath, fileName, localFilesStorage);
  }

  @Bean
  @StepScope
  public DataExportCsvItemReader dataExportItemReader(
    @Value("#{jobParameters['fileName']}") String fileName,
    @Value("#{stepExecutionContext['offset']}") Long offset,
    @Value("#{stepExecutionContext['limit']}") Long limit) {
    return new DataExportCsvItemReader(fileName, offset, limit, localFilesStorage);
  }

  @Bean
  @StepScope
  public AbstractStorageStreamWriter<Record, LocalFilesStorage> recordWriter(
    @Value("#{stepExecutionContext['tempOutputFilePath']}") String tempOutputFilePath) {
    return new MarcWriter(tempOutputFilePath, localFilesStorage);
  }

  @Bean
  @StepScope
  public ItemProcessor<ItemIdentifier, Record> processor(
    @Value("#{jobParameters['entityType']}") String entityType) {
    // TODO change to entityType values after schema update
    switch (entityType) {
    case "INSTANCE":
      return new MarcInstanceExportProcessor();
    case "HOLDINGS":
      return new MarcHoldingsExportProcessor();
    case "AUTHORITY":
      return new MarcAuthorityExportProcessor();
    default:
      throw new NonSupportedEntityException(entityType + " is not supported");
    }
  }
}
