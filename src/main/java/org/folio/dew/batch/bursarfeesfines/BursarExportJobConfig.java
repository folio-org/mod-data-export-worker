package org.folio.dew.batch.bursarfeesfines;

import java.util.Arrays;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.batch.bursarfeesfines.service.BursarExportUtils;
import org.folio.dew.batch.bursarfeesfines.service.BursarWriter;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.bursarfeesfines.AccountWithAncillaryData;
import org.folio.dew.domain.dto.bursarfeesfines.AggregatedAccountsByUser;
import org.folio.dew.repository.LocalFilesStorage;
import org.folio.dew.repository.S3CompatibleResource;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.listener.ExecutionContextPromotionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.CompositeItemProcessor;
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
  public Job bursarExportJob(Step prepareContext, Step exportStepRegular, Step exportStepAggregate, Step transferStep,
      JobRepository jobRepository, JobExecutionListener jobCompletionNotificationListener) {
    Flow regularFlow = new FlowBuilder<Flow>("mainFlow").start(exportStepRegular)
      .next(transferStep)
      .build();

    Flow aggregateFlow = new FlowBuilder<Flow>("alternateFlow").start(exportStepAggregate)
      .next(transferStep)
      .build();

    return new JobBuilder(ExportType.BURSAR_FEES_FINES.toString(), jobRepository).incrementer(new RunIdIncrementer())
      .listener(jobCompletionNotificationListener)
      .start(prepareContext)
      .on("IS AGGREGATE")
      .to(aggregateFlow)
      .from(prepareContext)
      .on("*")
      .to(regularFlow)
      .end()
      .build();
  }

  @Bean
  public Step prepareContext(JobRepository jobRepository, PrepareContextTasklet contextStep,
      PlatformTransactionManager transactionManager) {
    return new StepBuilder(BursarExportUtils.GET_FILENAME_STEP, jobRepository).tasklet(contextStep, transactionManager)
      .build();
  }

  @Bean
  public Step exportStepRegular(ItemReader<AccountWithAncillaryData> reader,
      ItemProcessor<AccountWithAncillaryData, AccountWithAncillaryData> filterer,
      ItemProcessor<AccountWithAncillaryData, String> formatter, @Qualifier("bursarWriter") ItemWriter<String> writer,
      BursarExportStepListener listener, JobRepository jobRepository, PlatformTransactionManager transactionManager) {
    log.info("Starting regular (non-aggregate) bursar export flow");
    CompositeItemProcessor<AccountWithAncillaryData, String> compositeProcessor = new CompositeItemProcessor<>();
    compositeProcessor.setDelegates(Arrays.asList(filterer, formatter));

    return new StepBuilder(BursarExportUtils.EXPORT_STEP, jobRepository)
      .<AccountWithAncillaryData, String>chunk(CHUNK_SIZE, transactionManager)
      .reader(reader)
      .processor(compositeProcessor)
      .writer(writer)
      .listener(promotionListener())
      .listener(listener)
      .listener(reader)
      .listener(formatter)
      .listener(filterer)
      .build();
  }

  @Bean
  public Step exportStepAggregate(ItemReader<AggregatedAccountsByUser> reader,
      ItemProcessor<AggregatedAccountsByUser, AggregatedAccountsByUser> filterer,
      ItemProcessor<AggregatedAccountsByUser, String> formatter, @Qualifier("bursarWriter") ItemWriter<String> writer,
      BursarExportStepListener listener, JobRepository jobRepository, PlatformTransactionManager transactionManager) {
    log.info("Starting aggregate bursar export flow");
    CompositeItemProcessor<AggregatedAccountsByUser, String> compositeProcessor = new CompositeItemProcessor<>();
    compositeProcessor.setDelegates(Arrays.asList(filterer, formatter));

    return new StepBuilder(BursarExportUtils.EXPORT_STEP, jobRepository)
      .<AggregatedAccountsByUser, String>chunk(CHUNK_SIZE, transactionManager)
      .reader(reader)
      .processor(compositeProcessor)
      .writer(writer)
      .listener(promotionListener())
      .listener(listener)
      .listener(reader)
      .listener(formatter)
      .listener(filterer)
      .build();
  }

  @Bean
  public Step transferStep(JobRepository jobRepository, TransferFeesFinesTasklet tasklet,
      PlatformTransactionManager transactionManager) {
    return new StepBuilder("transferStep", jobRepository).tasklet(tasklet, transactionManager)
      .build();
  }

  @Bean
  public ExecutionContextPromotionListener promotionListener() {
    var listener = new ExecutionContextPromotionListener();
    listener.setKeys(new String[] { "accounts" });
    return listener;
  }

  @Bean("bursarWriter")
  @StepScope
  public BursarWriter writer(@Value("#{jobParameters['tempOutputFilePath']}") String tempOutputFilePath,
      @Value("#{jobExecutionContext['filename']}") String finalFilename, LocalFilesStorage localFilesStorage) {
    log.error("BursarExportJobConfig.writer needs updating!!");

    String filename = tempOutputFilePath + '_' + finalFilename;
    WritableResource exportFileResource = new S3CompatibleResource<>(filename, localFilesStorage);

    log.info("Creating file {}.", filename);

    BursarWriter writer = BursarWriter.builder()
      .resource(exportFileResource)
      .localFilesStorage(localFilesStorage)
      .build();
    writer.setName("bursarExportWriter");
    return writer;
  }
}
