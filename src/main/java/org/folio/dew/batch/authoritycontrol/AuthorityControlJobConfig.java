package org.folio.dew.batch.authoritycontrol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.batch.JobCompletionNotificationListener;
import org.folio.dew.config.properties.AuthorityControlJobProperties;
import org.folio.dew.domain.dto.AuthorityControlExportConfig;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.authority.control.AuthorityDataStatDto;
import org.folio.dew.domain.dto.authoritycontrol.AuthorityUpdateHeadingExportFormat;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Log4j2
@Configuration
@RequiredArgsConstructor
public class AuthorityControlJobConfig {
  private final AuthorityControlJobProperties jobProperties;
  private final StepBuilderFactory stepBuilderFactory;
  private final JobBuilderFactory jobBuilderFactory;
  private final ObjectMapper objectMapper;

  @Bean
  public Job getAuthHeadingJob(
    JobRepository jobRepository,
    JobCompletionNotificationListener jobCompletionNotificationListener,
    @Qualifier("getAuthHeadingStep") Step getAuthHeadingStep) {
    return jobBuilderFactory
      .get(ExportType.AUTH_HEADINGS_UPDATES.toString())
      .repository(jobRepository)
      .incrementer(new RunIdIncrementer())
      .listener(jobCompletionNotificationListener)
      .start(getAuthHeadingStep)
      .build();
  }

  @Bean("getAuthHeadingStep")
  public Step getAuthHeadingStep(AuthorityControlItemReader authorityControlItemReader,
                                 AuthorityControlCsvFileWriter authorityControlCsvFileWriter,
                                 AuthorityControlStepListener authorityControlStepListener,
                                 ItemProcessor<AuthorityDataStatDto, AuthorityUpdateHeadingExportFormat> authorityControlProcessor) {
    return stepBuilderFactory
      .get("getAuthHeadingStep")
      .<AuthorityDataStatDto, AuthorityUpdateHeadingExportFormat>chunk(jobProperties.getJobChunkSize())
      .reader(authorityControlItemReader)
      .processor(authorityControlProcessor)
      .writer(authorityControlCsvFileWriter)
      .listener(authorityControlStepListener)
      .build();
  }

  @Bean("authorityControlProcessor")
  public ItemProcessor<AuthorityDataStatDto, AuthorityUpdateHeadingExportFormat> authorityControlProcessor(
    AuthorityControlToExportFormatMapper mapper) {
    return mapper::convertToExportFormat;
  }

  @JobScope
  @Bean("authorityControlExportConfig")
  public AuthorityControlExportConfig exportConfig(
    @Value("#{jobParameters['authorityControlExportConfig']}") String exportConfigStr) throws JsonProcessingException {
    return objectMapper.readValue(exportConfigStr, AuthorityControlExportConfig.class);
  }
}
