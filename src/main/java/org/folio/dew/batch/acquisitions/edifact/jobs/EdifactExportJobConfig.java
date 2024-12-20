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

  public static final String POL_MEM_KEY = "poLineIds";

  private Job constructEdifactExportJob(JobBuilder jobBuilder,
                                        EdiExportJobCompletionListener ediExportJobCompletionListener,
                                        Step mapToEdifactOrdersStep,
                                        Step saveToFTPStep,
                                        Step saveToMinIOStep,
                                        Step createExportHistoryRecordsStep) {
    return jobBuilder.incrementer(new RunIdIncrementer())
      .listener(ediExportJobCompletionListener)
      .start(mapToEdifactOrdersStep)
      .next(saveToMinIOStep)
      .next(saveToFTPStep)
      .next(createExportHistoryRecordsStep)
      .build();
  }

  @Bean
  public Job edifactOrdersExportJob(EdiExportJobCompletionListener ediExportJobCompletionListener, JobRepository jobRepository,
                                    Step mapToEdifactOrdersStep, Step saveToFTPStep, Step saveToMinIOStep, Step createExportHistoryRecordsStep) {
    return constructEdifactExportJob(new JobBuilder(ExportType.EDIFACT_ORDERS_EXPORT.getValue(), jobRepository),
      ediExportJobCompletionListener, mapToEdifactOrdersStep, saveToFTPStep, saveToMinIOStep, createExportHistoryRecordsStep);
  }

  @Bean
  public Job edifactClaimsExportJob(EdiExportJobCompletionListener ediExportJobCompletionListener, JobRepository jobRepository,
                                    Step mapToEdifactClaimsStep, Step saveToFTPStep, Step saveToMinIOStep, Step createExportHistoryRecordsStep) {
    return constructEdifactExportJob(new JobBuilder(ExportType.CLAIMS.getValue(), jobRepository),
      ediExportJobCompletionListener, mapToEdifactClaimsStep, saveToFTPStep, saveToMinIOStep, createExportHistoryRecordsStep);
  }

  @Bean
  public Step mapToEdifactOrdersStep(MapToEdifactOrdersTasklet mapToEdifactOrdersTasklet, JobRepository jobRepository,
                                     PlatformTransactionManager transactionManager) {
    return new StepBuilder("mapToEdifactStep", jobRepository)
      .tasklet(mapToEdifactOrdersTasklet, transactionManager)
      .build();
  }

  @Bean
  public Step mapToEdifactClaimsStep(MapToEdifactClaimsTasklet mapToEdifactClaimsTasklet, JobRepository jobRepository,
                                     PlatformTransactionManager transactionManager) {
    return new StepBuilder("mapToEdifactStep", jobRepository)
      .tasklet(mapToEdifactClaimsTasklet, transactionManager)
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
