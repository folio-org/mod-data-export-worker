package org.folio.dew.batch.circulationlog;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.batch.AbstractStorageStreamWriter;
import org.folio.dew.batch.CsvFileAssembler;
import org.folio.dew.batch.CsvPartStepExecutionListener;
import org.folio.dew.batch.CsvWriter;
import org.folio.dew.batch.JobCompletionNotificationListener;
import org.folio.dew.client.AuditClient;
import org.folio.dew.domain.dto.CirculationLogExportFormat;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.LogRecord;
import org.folio.dew.repository.RemoteFilesStorage;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@Log4j2
@RequiredArgsConstructor
public class CirculationLogJobConfig {

  private static final int NUMBER_OF_CONCURRENT_TASK_EXECUTIONS = 10;

  private final AuditClient auditClient;
  private final RemoteFilesStorage remoteFilesStorage;

  @Value("${circLogFetchPerRequest}")
  private Integer QUANTITY_TO_RETRIEVE_PER_HTTP_REQUEST;

  @Bean
  public Job getCirculationLogJob(
      JobCompletionNotificationListener jobCompletionNotificationListener,
      @Qualifier("getCirculationLogStep") Step getCirculationLogStep,
      JobRepository jobRepository) {
    return new JobBuilder(ExportType.CIRCULATION_LOG.toString(), jobRepository)
        .repository(jobRepository)
        .incrementer(new RunIdIncrementer())
        .listener(jobCompletionNotificationListener)
        .flow(getCirculationLogStep)
        .end()
        .build();
  }

  @Bean("getCirculationLogStep")
  public Step getCirculationLogStep(
      @Qualifier("getCirculationLogPartStep") Step getCirculationLogPartStep,
      CirculationLogCsvPartitioner partitioner,
      @Qualifier("asyncTaskExecutor") TaskExecutor taskExecutor,
      CsvFileAssembler csvFileAssembler,
      JobRepository jobRepository) {
    return new StepBuilder("getCirculationLogChunkStep", jobRepository)
        .partitioner("getCirculationLogPartStep", partitioner)
//        .taskExecutor(taskExecutor) // At the moment, async mode does not work correctly.
        .step(getCirculationLogPartStep)
        .aggregator(csvFileAssembler)
        .build();
  }

  @Bean
  @StepScope
  public CirculationLogCsvPartitioner getCirculationLogPartitioner(
      @Value("#{jobParameters['offset']}") Long offset,
      @Value("#{jobParameters['limit']}") Long limit,
      @Value("#{jobParameters['tempOutputFilePath']}") String tempOutputFilePath,
      @Value("#{jobParameters['query']}") String query) {
    return new CirculationLogCsvPartitioner(offset, limit, tempOutputFilePath, auditClient, query);
  }

  @Bean("getCirculationLogPartStep")
  public Step getCirculationLogPartStep(
      CirculationLogCsvItemReader circulationLogCsvItemReader,
      @Qualifier("circulationLog") AbstractStorageStreamWriter<CirculationLogExportFormat, RemoteFilesStorage> flatFileItemWriter,
      CirculationLogItemProcessor circulationLogItemProcessor,
      CsvPartStepExecutionListener csvPartStepExecutionListener,
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager) {
    return new StepBuilder("getCirculationLogPartStep", jobRepository)
        .<LogRecord, CirculationLogExportFormat>chunk(100, transactionManager)
        .reader(circulationLogCsvItemReader)
        .processor(circulationLogItemProcessor)
        .writer(flatFileItemWriter)
        .faultTolerant()
        .allowStartIfComplete(false)
        .throttleLimit(NUMBER_OF_CONCURRENT_TASK_EXECUTIONS)
        .listener(csvPartStepExecutionListener)
        .build();
  }

  @Bean
  @StepScope
  public CirculationLogCsvItemReader reader(
      @Value("#{jobParameters['query']}") String query,
      @Value("#{stepExecutionContext['offset']}") Long offset,
      @Value("#{stepExecutionContext['limit']}") Long limit) {
    return new CirculationLogCsvItemReader(auditClient, query, offset, limit, QUANTITY_TO_RETRIEVE_PER_HTTP_REQUEST);
  }

  @Bean("circulationLog")
  @StepScope
  public AbstractStorageStreamWriter<CirculationLogExportFormat, RemoteFilesStorage> writer(
      @Value("#{stepExecutionContext['tempOutputFilePath']}") String tempOutputFilePath) {
    return new CsvWriter<>(tempOutputFilePath,
      "User barcode,Item barcode,Object,Circ action,Date,Service point,Source,Description",
      new String[]{"userBarcode", "items", "objectField", "action", "date", "servicePointId", "source", "description"},
      (field, i) -> field, remoteFilesStorage);
  }

}
