package org.folio.dew.batch.eholdings;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ArrayUtils;
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

import org.folio.dew.batch.CsvPartStepExecutionListener;
import org.folio.dew.batch.CsvWriter;
import org.folio.dew.batch.JobCompletionNotificationListener;
import org.folio.dew.client.KbEbscoClient;
import org.folio.dew.domain.dto.EHoldingsResourceExportFormat;
import org.folio.dew.domain.dto.ExportType;

@Log4j2
@Configuration
@RequiredArgsConstructor
public class EHoldingsJobConfig {

  private static final int PROCESSING_RECORD_CHUNK_SIZE = 5;

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;
  private final KbEbscoClient kbEbscoClient;

  @Bean
  public Job getEHoldingsJob(
    JobRepository jobRepository,
    JobCompletionNotificationListener jobCompletionNotificationListener,
    @Qualifier("getEHoldingsStep") Step getEHoldingsStep) {
    return jobBuilderFactory
      .get(ExportType.E_HOLDINGS.toString())
      .repository(jobRepository)
      .incrementer(new RunIdIncrementer())
      .listener(jobCompletionNotificationListener)
      .start(getEHoldingsStep)
      .build();
  }

  @Bean("getEHoldingsStep")
  public Step getEHoldingsPartStep(
    @Qualifier("eHoldingsReader") EHoldingsPaginatedReader eHoldingsCsvItemReader,
    @Qualifier("eHoldingsWriter") FlatFileItemWriter<EHoldingsResourceExportFormat> flatFileItemWriter,
    EHoldingsItemProcessor eHoldingsItemProcessor,
    CsvPartStepExecutionListener csvPartStepExecutionListener) {
    return stepBuilderFactory.get("getEHoldingsStep")
      .<EHoldingsResourceExportFormat, EHoldingsResourceExportFormat>chunk(PROCESSING_RECORD_CHUNK_SIZE)
      .reader(eHoldingsCsvItemReader)
      .processor(eHoldingsItemProcessor)
      .writer(flatFileItemWriter)
      .listener(csvPartStepExecutionListener)
      .build();
  }

  @Bean("eHoldingsReader")
  @StepScope
  public EHoldingsPaginatedReader reader(
    @Value("#{jobParameters['titleFields']}") String titleFields,
    @Value("#{jobParameters['titleSearchFilters']}") String titleSearchFilters) {
    return new EHoldingsPaginatedReader(kbEbscoClient, titleFields, titleSearchFilters);
  }

  @Bean("eHoldingsWriter")
  @StepScope
  public FlatFileItemWriter<EHoldingsResourceExportFormat> writer(
    @Value("#{stepExecutionContext['tempOutputFilePath']}") String tempOutputFilePath,
    @Value("#{stepExecutionContext['partition']}") Long partition,
    @Value("#{stepExecutionContext['packageFields']}") String packageFields,
    @Value("#{stepExecutionContext['titleFields']}") String titleFields) {
    return new CsvWriter<>(tempOutputFilePath, partition,
      packageFields + ',' + titleFields,
      ArrayUtils.addAll(packageFields.split(","), titleFields.split(",")),
      (field, i) -> field);
  }
}
