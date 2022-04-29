package org.folio.dew.batch.eholdings;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;

import org.folio.dew.batch.CsvFileAssembler;
import org.folio.dew.batch.CsvPartStepExecutionListener;
import org.folio.dew.batch.CsvWriter;
import org.folio.dew.batch.JobCompletionNotificationListener;
import org.folio.dew.client.KbEbscoClient;
import org.folio.dew.domain.dto.EHoldingsExportFormat;
import org.folio.dew.domain.dto.EHoldingsRecord;
import org.folio.dew.domain.dto.ExportType;

@Log4j2
@Configuration
@RequiredArgsConstructor
public class EHoldingsJobConfig {

  private static final int NUMBER_OF_CONCURRENT_TASK_EXECUTIONS = 10;

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;
  private final KbEbscoClient kbEbscoClient;

  @Bean
  public Job getEHoldingsJob(
    JobCompletionNotificationListener jobCompletionNotificationListener,
    @Qualifier("getEHoldingsStep") Step getEHoldingsStep,
    JobRepository jobRepository) {
    return jobBuilderFactory
      .get(ExportType.E_HOLDINGS.toString())
      .repository(jobRepository)
      .incrementer(new RunIdIncrementer())
      .listener(jobCompletionNotificationListener)
      .flow(getEHoldingsStep)
      .end()
      .build();
  }

  @Bean("getEHoldingsStep")
  public Step getEHoldingsStep(
    @Qualifier("getEHoldingsPartStep") Step getEHoldingsPartStep,
    EHoldingsPartitioner partitioner,
    @Qualifier("asyncTaskExecutor") TaskExecutor taskExecutor,
    CsvFileAssembler csvFileAssembler) {
    return stepBuilderFactory
      .get("getEHoldingsChunkStep")
      .partitioner("getEHoldingsPartStep", partitioner)
      .taskExecutor(taskExecutor)
      .step(getEHoldingsPartStep)
      .aggregator(csvFileAssembler)
      .build();
  }

  @Bean
  @StepScope
  public EHoldingsPartitioner getEHoldingsPartitioner(
    @Value("#{jobParameters['offset']}") Long offset,
    @Value("#{jobParameters['limit']}") Long limit,
    @Value("#{jobParameters['tempOutputFilePath']}") String tempOutputFilePath,
    @Value("#{jobParameters['recordId']}") String recordId,
    @Value("#{jobParameters['recordType']}") String recordType,
    @Value("#{jobParameters['titleSearchFilters']}") String titleSearchFilters) {
    return new EHoldingsPartitioner(offset, limit, tempOutputFilePath, kbEbscoClient, recordId, recordType, titleSearchFilters);
  }

  @Bean("getEHoldingsPartStep")
  public Step getEHoldingsPartStep(
    @Qualifier("eHoldingsReader") EHoldingsCsvItemReader eHoldingsCsvItemReader,
    @Qualifier("eHoldingsWriter") FlatFileItemWriter<EHoldingsExportFormat> flatFileItemWriter,
    EHoldingsItemProcessor eHoldingsItemProcessor,
    CsvPartStepExecutionListener csvPartStepExecutionListener) {
    return stepBuilderFactory
      .get("getEHoldingsPartStep")
      .<EHoldingsRecord, EHoldingsExportFormat>chunk(100)
      .reader(eHoldingsCsvItemReader)
      .processor(eHoldingsItemProcessor)
      .writer(flatFileItemWriter)
      .faultTolerant()
      .allowStartIfComplete(false)
      .throttleLimit(NUMBER_OF_CONCURRENT_TASK_EXECUTIONS)
      .listener(csvPartStepExecutionListener)
      .build();
  }

  @Bean("eHoldingsReader")
  @StepScope
  public EHoldingsCsvItemReader reader(
    @Value("#{jobParameters['offset']}") Long offset,
    @Value("#{jobParameters['limit']}") Long limit,
    @Value("#{jobParameters['recordId']}") String recordId,
    @Value("#{jobParameters['recordType']}") String recordType,
    @Value("#{jobParameters['titleSearchFilters']}") String titleSearchFilters) {
    return new EHoldingsCsvItemReader(offset, limit, kbEbscoClient, recordId, recordType, titleSearchFilters);
  }

  @Bean("eHoldingsWriter")
  @StepScope
  public FlatFileItemWriter<EHoldingsExportFormat> writer(
    @Value("#{stepExecutionContext['tempOutputFilePath']}") String tempOutputFilePath,
    @Value("#{stepExecutionContext['partition']}") Long partition,
    @Value("#{stepExecutionContext['packageFields']}") String packageFields,
    @Value("#{stepExecutionContext['titleFields']}") String titleFields) {
    return new CsvWriter<>(tempOutputFilePath, partition,
      packageFields + ',' + titleFields,
      new String[]{packageFields + ',' + titleFields},
      (field, i) -> field);
  }
}
