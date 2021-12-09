package org.folio.dew.batch.bursarfeesfines;

import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.batch.bursarfeesfines.service.BursarFeesFinesUtils;
import org.folio.dew.domain.dto.Account;
import org.folio.dew.domain.dto.Feefineaction;
import org.folio.dew.domain.dto.bursarfeesfines.BursarFormat;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.listener.ExecutionContextPromotionListener;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileHeaderCallback;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

@Configuration
@Log4j2
public class BursarExportJobConfig {

  @Bean
  public Job bursarExportJob(
      Step exportChargeFeefinesStep,
      Step exportRefundFeefinesStep,
      Step transferFeefinesStep,
      JobBuilderFactory jobBuilderFactory,
      JobExecutionListener jobCompletionNotificationListener) {
    return jobBuilderFactory
        .get(ExportType.BURSAR_FEES_FINES.toString())
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
      StepBuilderFactory stepBuilderFactory) {
    return stepBuilderFactory
        .get(BursarFeesFinesUtils.CHARGE_FEESFINES_EXPORT_STEP)
        .<Account, BursarFormat>chunk(1000)
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
      StepBuilderFactory stepBuilderFactory) {
    return stepBuilderFactory
        .get(BursarFeesFinesUtils.REFUND_FEESFINES_EXPORT_STEP)
        .<Feefineaction, BursarFormat>chunk(1000)
        .reader(reader)
        .processor(processor)
        .writer(writer)
        .listener(listener)
        .build();
  }

  @Bean
  public Step transferFeefinesStep(StepBuilderFactory stepBuilderFactory, TransferFeesFinesTasklet tasklet) {
    return stepBuilderFactory.get("transferFeefinesStep").tasklet(tasklet).build();
  }

  @Bean
  public ExecutionContextPromotionListener promotionListener() {
    var listener = new ExecutionContextPromotionListener();
    listener.setKeys(new String[] {"accounts", "userIdMap"});
    return listener;
  }

  @Bean("bursarFeesFines")
  @StepScope
  public FlatFileItemWriter<BursarFormat> writer(
      @Value("#{jobParameters['tempOutputFilePath']}") String tempOutputFilePath,
      @Value("#{stepExecution.stepName}") String stepName) {
    String fileName = tempOutputFilePath + '_' + BursarFeesFinesUtils.getFilename(stepName);
    Resource exportFileResource = new FileSystemResource(fileName);

    var fieldNames =
        new String[] {
          "employeeId", "amount", "itemType", "transactionDate", "sfs", "termValue", "description"
        };
    var lineFormat = "%11.11s%9.9s%12.12s%6.6s%3.3s%4.4s%30.30s";
    FlatFileHeaderCallback header = writer -> writer.write("LIB02");
    log.info("Creating file {}.", fileName);
    return new FlatFileItemWriterBuilder<BursarFormat>()
        .name("bursarExportWriter")
        .headerCallback(header)
        .formatted()
        .format(lineFormat)
        .names(fieldNames)
        .resource(exportFileResource)
        .build();
  }
}
