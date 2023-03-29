package org.folio.dew.batch.authoritycontrol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.batch.JobCompletionNotificationListener;
import org.folio.dew.batch.authoritycontrol.readers.AuthUpdateHeadingsItemReader;
import org.folio.dew.batch.authoritycontrol.readers.LinkedBibUpdateItemReader;
import org.folio.dew.config.properties.AuthorityControlJobProperties;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.authority.control.AuthorityControlExportConfig;
import org.folio.dew.domain.dto.authority.control.AuthorityDataStatDto;
import org.folio.dew.domain.dto.authority.control.InstanceDataStatDto;
import org.folio.dew.domain.dto.authoritycontrol.exportformat.AuthUpdateHeadingExportFormat;
import org.folio.dew.domain.dto.authoritycontrol.exportformat.FailedLinkedBibExportFormat;
import org.folio.dew.repository.LocalFilesStorage;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Log4j2
@Configuration
@RequiredArgsConstructor
public class AuthorityControlJobConfig {
  private final AuthorityControlJobProperties jobProperties;
  private final ObjectMapper objectMapper;

  @Bean
  public Job getAuthHeadingJob(JobRepository jobRepository,
                               JobCompletionNotificationListener jobCompletionNotificationListener,
                               @Qualifier("getAuthHeadingStep") Step getAuthHeadingStep) {
    return new JobBuilder(ExportType.AUTH_HEADINGS_UPDATES.toString(), jobRepository)
      .incrementer(new RunIdIncrementer())
      .listener(jobCompletionNotificationListener)
      .start(getAuthHeadingStep)
      .build();
  }

  @Bean
  public Job getFailedLinkedBibJob(JobRepository jobRepository,
                                   JobCompletionNotificationListener jobCompletionNotificationListener,
                                   @Qualifier("getFailedLinkedBibStep") Step getFailedLinkedBibStep) {
    return new JobBuilder(ExportType.FAILED_LINKED_BIB_UPDATES.toString(), jobRepository)
      .incrementer(new RunIdIncrementer())
      .listener(jobCompletionNotificationListener)
      .start(getFailedLinkedBibStep)
      .build();
  }

  @Bean("getAuthHeadingStep")
  public Step getAuthHeadingStep(AuthUpdateHeadingsItemReader authUpdateHeadingsItemReader,
                                 @Qualifier("authUpdateHeadingWriter") AuthorityControlCsvFileWriter writer,
                                 AuthorityControlStepListener authorityControlStepListener,
                                 ItemProcessor<AuthorityDataStatDto, AuthUpdateHeadingExportFormat> authUpdateHeadingProcessor,
                                 JobRepository jobRepository,
                                 PlatformTransactionManager transactionManager) {
    return new StepBuilder("getAuthHeadingStep", jobRepository)
      .<AuthorityDataStatDto, AuthUpdateHeadingExportFormat>chunk(jobProperties.getJobChunkSize(), transactionManager)
      .reader(authUpdateHeadingsItemReader)
      .processor(authUpdateHeadingProcessor)
      .writer(writer)
      .listener(authorityControlStepListener)
      .build();
  }

  @Bean("getFailedLinkedBibStep")
  public Step getFailedLinkedBibStep(LinkedBibUpdateItemReader linkedBibUpdateItemReader,
                                     @Qualifier("failedLinkedBibWriter") AuthorityControlCsvFileWriter writer,
                                     AuthorityControlStepListener authorityControlStepListener,
                                     ItemProcessor<InstanceDataStatDto, FailedLinkedBibExportFormat> failedLinkedBibProcessor,
                                     JobRepository jobRepository,
                                     PlatformTransactionManager transactionManager) {
    return new StepBuilder("getFailedLinkedBibStep", jobRepository)
      .<InstanceDataStatDto, FailedLinkedBibExportFormat>chunk(jobProperties.getJobChunkSize(), transactionManager)
      .reader(linkedBibUpdateItemReader)
      .processor(failedLinkedBibProcessor)
      .writer(writer)
      .listener(authorityControlStepListener)
      .build();
  }

  @Bean("authUpdateHeadingProcessor")
  public ItemProcessor<AuthorityDataStatDto, AuthUpdateHeadingExportFormat> authUpdateHeadingProcessor(
    AuthorityControlToExportFormatMapper mapper) {
    return mapper::convertToAuthUpdateHeadingsExportFormat;
  }

  @Bean("failedLinkedBibProcessor")
  public ItemProcessor<InstanceDataStatDto, FailedLinkedBibExportFormat> failedLinkedBibProcessor(
    AuthorityControlToExportFormatMapper mapper) {
    return mapper::convertToFailedLinkedBibExportFormat;
  }

  @StepScope
  @Bean("authUpdateHeadingWriter")
  public AuthorityControlCsvFileWriter authUpdateHeadingWriter(
    @Value("#{jobParameters['tempOutputFilePath']}") String tempOutputFilePath, LocalFilesStorage localFilesStorage) {
    return new AuthorityControlCsvFileWriter(AuthUpdateHeadingExportFormat.class, tempOutputFilePath,
      localFilesStorage);
  }

  @StepScope
  @Bean("failedLinkedBibWriter")
  public AuthorityControlCsvFileWriter failedLinkedBibWriter(
    @Value("#{jobParameters['tempOutputFilePath']}") String tempOutputFilePath, LocalFilesStorage localFilesStorage) {
    return new AuthorityControlCsvFileWriter(FailedLinkedBibExportFormat.class, tempOutputFilePath, localFilesStorage);
  }

  @JobScope
  @Bean("authorityControlExportConfig")
  public AuthorityControlExportConfig exportConfig(
    @Value("#{jobParameters['authorityControlExportConfig']}") String exportConfigStr) throws JsonProcessingException {
    if (exportConfigStr == null) {
      return new AuthorityControlExportConfig();
    }
    return objectMapper.readValue(exportConfigStr, AuthorityControlExportConfig.class);
  }
}
