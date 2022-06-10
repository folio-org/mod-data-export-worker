package org.folio.me.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.batch.CsvFileAssembler;
import org.folio.dew.batch.CsvPartStepExecutionListener;
import org.folio.dew.batch.JobCompletionNotificationListener;
import org.folio.dew.domain.dto.ItemIdentifier;
import org.folio.dew.error.NonSupportedEntityException;
import org.marc4j.marc.Record;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;

@Configuration
@Log4j2
@RequiredArgsConstructor
public class DataExportJobConfig {
  private static final int POOL_SIZE = 10;
  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;

  @Bean
  public Job dataExportJob(
      JobCompletionNotificationListener jobCompletionNotificationListener,
      Step dataExportStep,
      JobRepository jobRepository) {
    return jobBuilderFactory
        .get("MARC_EXPORT") // TODO change to ExportType.MARC_EXPORT.toString() after schema update
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
      CsvFileAssembler csvFileAssembler) {
    return stepBuilderFactory
        .get("dataExportChunkStep")
        .partitioner("dataExportPartitionStep", partitioner)
        .taskExecutor(asyncTaskExecutor)
        .step(dataExportPartitionStep)
        .aggregator(csvFileAssembler)
        .build();
  }

  @Bean
  public Step dataExportPartitionStep(
    DataExportCsvItemReader dataExportCsvItemReader,
    FlatFileItemWriter<Record> recordWriter,
    ItemProcessor<ItemIdentifier, Record> processor,
    CsvPartStepExecutionListener csvPartStepExecutionListener
  ) {
    return stepBuilderFactory
      .get("dataExportPartitionStep")
      .<ItemIdentifier, Record>chunk(100)
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
    return new DataExportCsvPartitioner(offset, limit, tempOutputFilePath, fileName);
  }

  @Bean
  @StepScope
  public DataExportCsvItemReader dataExportItemReader(
    @Value("#{jobParameters['fileName']}") String fileName,
    @Value("#{stepExecutionContext['offset']}") Long offset,
    @Value("#{stepExecutionContext['limit']}") Long limit) {
    return new DataExportCsvItemReader(fileName, offset, limit);
  }

  @Bean
  @StepScope
  public FlatFileItemWriter<Record> recordWriter(
    @Value("#{stepExecutionContext['tempOutputFilePath']}") String tempOutputFilePath) {
    return new MarcWriter(tempOutputFilePath);
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
