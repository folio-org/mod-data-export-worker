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
      .flow(getEHoldingsStep)
      .end()
      .build();
  }

  @Bean("getEHoldingsStep")
  public Step getEHoldingsStep(
    @Qualifier("eHoldingsReader") EHoldingsItemReader eHoldingsCsvItemReader,
    @Qualifier("eHoldingsWriter") FlatFileItemWriter<EHoldingsResourceExportFormat> flatFileItemWriter,
    EHoldingsItemProcessor eHoldingsItemProcessor,
    EHoldingsStepListener eHoldingsStepListener) {
    return stepBuilderFactory
      .get("getEHoldingsStep")
      .<EHoldingsResourceExportFormat, EHoldingsResourceExportFormat>chunk(PROCESSING_RECORD_CHUNK_SIZE)
      .reader(eHoldingsCsvItemReader)
      .processor(eHoldingsItemProcessor)
      .writer(flatFileItemWriter)
      .listener(eHoldingsStepListener)
      .build();
  }

  @Bean("eHoldingsReader")
  @StepScope
  public EHoldingsItemReader reader(
    @Value("#{jobParameters['titleFields']}") String titleFields) {
    return new EHoldingsItemReader(kbEbscoClient, titleFields);
  }

  @Bean("eHoldingsWriter")
  @StepScope
  public FlatFileItemWriter<EHoldingsResourceExportFormat> writer(
    @Value("#{jobParameters['tempOutputFilePath']}") String tempOutputFilePath,
    @Value("#{stepExecutionContext['partition']}") Long partition,
    @Value("#{jobParameters['packageFields']}") String packageFields,
    @Value("#{jobParameters['titleFields']}") String titleFields) {
    String headers = packageFields;
    String[] fields = packageFields.split(",");
    if (!titleFields.isBlank()) {
      if (!packageFields.isBlank()) {
        headers += ',';
        ArrayUtils.addAll(fields, titleFields.split(","));
      }
      headers += titleFields;
      fields = titleFields.split(",");
    }
    return new CsvWriter<>(tempOutputFilePath, partition, headers, fields, (field, i) -> field);
  }
}
