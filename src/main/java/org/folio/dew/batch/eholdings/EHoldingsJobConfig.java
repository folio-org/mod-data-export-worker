package org.folio.dew.batch.eholdings;

import java.util.ArrayList;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import org.folio.dew.batch.JobCompletionNotificationListener;
import org.folio.dew.client.KbEbscoClient;
import org.folio.dew.domain.dto.EHoldingsExportConfig;
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
  private final ObjectMapper objectMapper;

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
    @Qualifier("eHoldingsWriter") FlatFileItemWriter<EHoldingsResourceExportFormat> flatFileItemWriter,
    EHoldingsItemReader eHoldingsCsvItemReader,
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
    @Value("#{jobParameters['eHoldingsExportConfig']}") String exportConfigStr) throws JsonProcessingException {
    var eHoldingsExportConfig = objectMapper.readValue(exportConfigStr, EHoldingsExportConfig.class);
    return new EHoldingsItemReader(kbEbscoClient, eHoldingsExportConfig);
  }

  @Bean("eHoldingsWriter")
  @StepScope
  public FlatFileItemWriter<EHoldingsResourceExportFormat> writer(
    @Value("#{jobParameters['tempOutputFilePath']}") String tempOutputFilePath,
    @Value("#{jobParameters['eHoldingsExportConfig']}") String exportConfigStr) throws JsonProcessingException {
    var eHoldingsExportConfig = objectMapper.readValue(exportConfigStr, EHoldingsExportConfig.class);

    var exportFields = new ArrayList<String>();
    if (eHoldingsExportConfig.getPackageFields() != null) {
      exportFields.addAll(eHoldingsExportConfig.getPackageFields());
    }
    if (eHoldingsExportConfig.getTitleFields() != null) {
      exportFields.addAll(eHoldingsExportConfig.getTitleFields());
    }
    if (exportFields.isEmpty()) {
      throw new IllegalArgumentException("Export fields is empty");
    }

    var headers = String.join(",", exportFields);

    return new FlatFileItemWriterBuilder<EHoldingsResourceExportFormat>()
      .name("eHoldingsWriter")
      .resource(new FileSystemResource(tempOutputFilePath))
      .delimited()
      .delimiter(",")
      .names(exportFields.toArray(String[]::new))
      .headerCallback(writer -> writer.write(headers))
      .build();
  }
}
