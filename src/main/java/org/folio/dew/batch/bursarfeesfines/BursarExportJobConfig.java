package org.folio.dew.batch.bursarfeesfines;

import lombok.extern.log4j.Log4j2;
import org.folio.dew.batch.bursarfeesfines.service.BursarFeesFinesUtils;
import org.folio.dew.batch.bursarfeesfines.service.BursarWriter;
import org.folio.dew.batch.bursarfeesfines.service.BursarWriterBuilder;
import org.folio.dew.domain.dto.Account;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.Feefineaction;
import org.folio.dew.domain.dto.bursarfeesfines.BursarFormat;
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

  @Bean
  public Job bursarExportJob(
      Step exportChargeFeefinesStep,
      Step exportRefundFeefinesStep,
      Step transferFeefinesStep,
      JobRepository jobRepository,
      JobExecutionListener jobCompletionNotificationListener) {
    return new JobBuilder(ExportType.BURSAR_FEES_FINES.toString(), jobRepository)
        .incrementer(new RunIdIncrementer())
        .listener(jobCompletionNotificationListener)
        .flow(exportChargeFeefinesStep)
        .next(exportRefundFeefinesStep)
        .next(transferFeefinesStep)
        .end()
        .build();
  }

  @Bean
  public Step exportChargeFeefinesStep(
      ItemReader<Account> reader,
      ItemProcessor<Account, BursarFormat> processor,
      @Qualifier("bursarFeesFines") ItemWriter<BursarFormat> writer,
      BursarExportStepListener listener,
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager) {
    return new StepBuilder(BursarFeesFinesUtils.CHARGE_FEESFINES_EXPORT_STEP, jobRepository)
        .<Account, BursarFormat>chunk(1000, transactionManager)
        .reader(reader)
        .processor(processor)
        .writer(writer)
        .listener(promotionListener())
        .listener(listener)
        .build();
  }

  @Bean
  public Step exportRefundFeefinesStep(
      ItemReader<Feefineaction> reader,
      ItemProcessor<Feefineaction, BursarFormat> processor,
      @Qualifier("bursarFeesFines") ItemWriter<BursarFormat> writer,
      BursarExportStepListener listener,
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager) {
    return new StepBuilder(BursarFeesFinesUtils.REFUND_FEESFINES_EXPORT_STEP, jobRepository)
        .<Feefineaction, BursarFormat>chunk(1000, transactionManager)
        .reader(reader)
        .processor(processor)
        .writer(writer)
        .listener(listener)
        .build();
  }

  @Bean
  public Step transferFeefinesStep(JobRepository jobRepository, TransferFeesFinesTasklet tasklet, PlatformTransactionManager transactionManager) {
    return new StepBuilder("transferFeefinesStep", jobRepository).tasklet(tasklet, transactionManager).build();
  }

  @Bean
  public ExecutionContextPromotionListener promotionListener() {
    var listener = new ExecutionContextPromotionListener();
    listener.setKeys(new String[] {"accounts", "userIdMap"});
    return listener;
  }

  @Bean("bursarFeesFines")
  @StepScope
  public BursarWriter<BursarFormat> writer(
      @Value("#{jobParameters['tempOutputFilePath']}") String tempOutputFilePath,
      @Value("#{stepExecution.stepName}") String stepName,
      LocalFilesStorage localFilesStorage) {
    String fileName = tempOutputFilePath + '_' + BursarFeesFinesUtils.getFilename(stepName);
    WritableResource exportFileResource = new S3CompatibleResource<>(fileName, localFilesStorage);

    var fieldNames =
        new String[] {
          "employeeId", "amount", "itemType", "transactionDate", "sfs", "termValue", "description"
        };
    var lineFormat = "%11.11s%9.9s%12.12s%6.6s%3.3s%4.4s%30.30s";
    var header = "LIB02";
    log.info("Creating file {}.", fileName);

    return new BursarWriterBuilder<BursarFormat>()
        .name("bursarExportWriter")
        .header(header)
        .formatted()
        .format(lineFormat)
        .names(fieldNames)
        .resource(exportFileResource)
        .localFilesStorage(localFilesStorage)
        .build();
  }
}
