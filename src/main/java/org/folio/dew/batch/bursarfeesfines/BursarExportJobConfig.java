package org.folio.dew.batch.bursarfeesfines;

import lombok.extern.log4j.Log4j2;
import org.folio.dew.batch.bursarfeesfines.service.BursarFeesFinesUtils;
import org.folio.dew.batch.bursarfeesfines.service.BursarWriter;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.bursarfeesfines.AccountWithAncillaryData;
import org.folio.dew.repository.LocalFilesStorage;
import org.folio.dew.repository.S3CompatibleResource;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.listener.ExecutionContextPromotionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.WritableResource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@Log4j2
public class BursarExportJobConfig {

  private static final int CHUNK_SIZE = 1000;

  @Bean
  public Job bursarExportJob(
    Step exportStep,
    Step transferStep,
    JobRepository jobRepository,
    JobExecutionListener jobCompletionNotificationListener
  ) {
    return new JobBuilder(
      ExportType.BURSAR_FEES_FINES.toString(),
      jobRepository
    )
      .incrementer(new RunIdIncrementer())
      .listener(jobCompletionNotificationListener)
      .flow(exportStep)
      .next(transferStep)
      .end()
      .build();
  }

  @Bean
  public Step exportStep(
    ItemReader<AccountWithAncillaryData> reader,
    ItemProcessor<AccountWithAncillaryData, String> processor,
    @Qualifier("bursarFeesFines") ItemWriter<String> writer,
    BursarExportStepListener listener,
    JobRepository jobRepository,
    PlatformTransactionManager transactionManager
  ) {
    return new StepBuilder(BursarFeesFinesUtils.EXPORT_STEP, jobRepository)
      .<AccountWithAncillaryData, String>chunk(CHUNK_SIZE, transactionManager)
      .reader(reader)
      .processor(processor)
      .writer(writer)
      .listener(promotionListener())
      .listener(listener)
      .build();
  }

  @Bean
  public Step transferStep(
    JobRepository jobRepository,
    TransferFeesFinesTasklet tasklet,
    PlatformTransactionManager transactionManager
  ) {
    return new StepBuilder("transferStep", jobRepository)
      .tasklet(tasklet, transactionManager)
      .build();
  }

  @Bean
  public ExecutionContextPromotionListener promotionListener() {
    var listener = new ExecutionContextPromotionListener();
    listener.setKeys(new String[] { "accounts", "userIdMap" });
    return listener;
  }

  @Bean("bursarFeesFines")
  @StepScope
  public BursarWriter writer(
    @Value("#{jobParameters['tempOutputFilePath']}") String tempOutputFilePath,
    @Value("#{stepExecution.stepName}") String stepName,
    LocalFilesStorage localFilesStorage
  ) {
    log.error("BursarExportJobConfig.writer needs updating!!");
    String fileName =
      tempOutputFilePath + '_' + BursarFeesFinesUtils.getFilename();
    WritableResource exportFileResource = new S3CompatibleResource<>(
      fileName,
      localFilesStorage
    );

    log.info("Creating file {}.", fileName);

    BursarWriter writer = BursarWriter
      .builder()
      .header("HEADER GOES HERE")
      .footer("FOOTER GOES HERE")
      .resource(exportFileResource)
      .localFilesStorage(localFilesStorage)
      .build();
    writer.setName("bursarExportWriter");
    return writer;
  }
}
