package org.folio.dew.batch.acquisitions.edifact.jobs;

import org.folio.dew.batch.JobCompletionNotificationListener;
import org.folio.dew.domain.dto.ExportType;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Configuration
@Log4j2
@RequiredArgsConstructor
public class EdifactExportJobConfig {

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;

  @Bean
  public Job edifactExportJob(
    JobCompletionNotificationListener jobCompletionNotificationListener,
    JobRepository jobRepository,
    Step mapToEdifactStep,
    Step saveToFTPStep,
    Step saveToMinIOStep,
    Step createExportHistoryRecordsStep) {
    return jobBuilderFactory
      .get(ExportType.EDIFACT_ORDERS_EXPORT.getValue())
      .repository(jobRepository)
      .incrementer(new RunIdIncrementer())
      .listener(jobCompletionNotificationListener)
      .start(mapToEdifactStep)
      .next(saveToMinIOStep)
      .next(saveToFTPStep)
      .next(createExportHistoryRecordsStep)
      .build();
  }

  @Bean
  public Step mapToEdifactStep(MapToEdifactTasklet mapToEdifactTasklet) {
    return stepBuilderFactory
      .get("mapToEdifactStep")
      .tasklet(mapToEdifactTasklet)
      .build();
  }

  @Bean
  public Step saveToMinIOStep(SaveToMinioTasklet saveToMinioTasklet) {
    return stepBuilderFactory
      .get("saveToMinIOStep")
      .tasklet(saveToMinioTasklet)
      .build();
  }

  @Bean
  public Step saveToFTPStep(SaveToFileStorageTasklet saveToFileStorageTasklet) {
    return stepBuilderFactory
      .get("saveToFTPStep")
      .tasklet(saveToFileStorageTasklet)
      .build();
  }

  @Bean
  public Step createExportHistoryRecordsStep(ExportHistoryTasklet exportHistoryTasklet) {
    return stepBuilderFactory
      .get("createExportHistoryRecordsStep")
      .tasklet(exportHistoryTasklet)
      .build();
  }

}
