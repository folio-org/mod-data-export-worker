package org.folio.dew.batch.acquisitions.edifact.jobs;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.ExportType;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@Log4j2
@RequiredArgsConstructor
public class EdifactExportJobConfig {
  @Bean
  public Job edifactExportJob(
    EdiExportJobCompletionListener ediExportJobCompletionListener,
    JobRepository jobRepository,
    Step mapToEdifactStep,
    Step saveToFTPStep,
    Step saveToMinIOStep,
    Step createExportHistoryRecordsStep) {
    return new JobBuilder(ExportType.EDIFACT_ORDERS_EXPORT.getValue(), jobRepository)
      .incrementer(new RunIdIncrementer())
      .listener(ediExportJobCompletionListener)
      .start(mapToEdifactStep)
      .next(saveToMinIOStep)
      .next(saveToFTPStep)
      .next(createExportHistoryRecordsStep)
      .build();
  }

  @Bean
  public Step mapToEdifactStep(MapToEdifactTasklet mapToEdifactTasklet, JobRepository jobRepository,
                               PlatformTransactionManager transactionManager) {
    return new StepBuilder("mapToEdifactStep", jobRepository)
      .tasklet(mapToEdifactTasklet, transactionManager)
      .build();
  }

  @Bean
  public Step saveToMinIOStep(SaveToMinioTasklet saveToMinioTasklet, JobRepository jobRepository,
                              PlatformTransactionManager transactionManager) {
    return new StepBuilder("saveToMinIOStep", jobRepository)
      .tasklet(saveToMinioTasklet, transactionManager)
      .build();
  }

  @Bean
  public Step saveToFTPStep(SaveToFileStorageTasklet saveToFileStorageTasklet, JobRepository jobRepository,
                            PlatformTransactionManager transactionManager) {
    return new StepBuilder("saveToFTPStep", jobRepository)
      .tasklet(saveToFileStorageTasklet, transactionManager)
      .build();
  }

  @Bean
  public Step createExportHistoryRecordsStep(ExportHistoryTasklet exportHistoryTasklet, JobRepository jobRepository,
                                             PlatformTransactionManager transactionManager) {
    return new StepBuilder("createExportHistoryRecordsStep", jobRepository)
      .tasklet(exportHistoryTasklet, transactionManager)
      .build();
  }

}
