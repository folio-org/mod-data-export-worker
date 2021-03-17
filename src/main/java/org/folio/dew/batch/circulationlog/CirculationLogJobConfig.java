package org.folio.dew.batch.circulationlog;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.des.domain.dto.ExportType;
import org.folio.dew.batch.CsvFileAssembler;
import org.folio.dew.batch.CsvPartStepExecutionListener;
import org.folio.dew.batch.CsvWriter;
import org.folio.dew.batch.JobCompletionNotificationListener;
import org.folio.dew.client.AuditClient;
import org.folio.dew.domain.dto.LogRecord;
import org.folio.dew.domain.dto.LogRecordItems;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;

import java.util.List;
import java.util.stream.Collectors;

@Configuration
@Log4j2
@RequiredArgsConstructor
public class CirculationLogJobConfig {

  private static final int NUMBER_OF_CONCURRENT_TASK_EXECUTIONS = 10;

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;
  private final AuditClient auditClient;

  @Bean
  public Job getCirculationLogJob(JobCompletionNotificationListener jobCompletionNotificationListener,
      @Qualifier("getCirculationLogStep") Step getCirculationLogStep, JobRepository jobRepository) {
    return jobBuilderFactory.get(ExportType.CIRCULATION_LOG.toString())
        .repository(jobRepository)
        .incrementer(new RunIdIncrementer())
        .listener(jobCompletionNotificationListener)
        .flow(getCirculationLogStep)
        .end()
        .build();
  }

  @Bean("getCirculationLogStep")
  public Step getCirculationLogStep(@Qualifier("getCirculationLogPartStep") Step getCirculationLogPartStep, Partitioner partitioner,
      @Qualifier("asyncTaskExecutor") TaskExecutor taskExecutor, CsvFileAssembler csvFileAssembler) {
    return stepBuilderFactory.get("getCirculationLogChunkStep")
        .partitioner("getCirculationLogPartStep", partitioner)
        .taskExecutor(taskExecutor)
        .step(getCirculationLogPartStep)
        .aggregator(csvFileAssembler)
        .build();
  }

  @Bean
  @StepScope
  public Partitioner getCirculationLogPartitioner(@Value("#{jobParameters['offset']}") Long offset,
      @Value("#{jobParameters['limit']}") Long limit, @Value("#{jobParameters['tempOutputFilePath']}") String tempOutputFilePath,
      @Value("#{jobParameters['query']}") String query) {
    return new CirculationLogCsvPartitioner(offset, limit, tempOutputFilePath, auditClient, query);
  }

  @Bean("getCirculationLogPartStep")
  public Step getCirculationLogPartStep(CirculationLogCsvItemReader circulationLogCsvItemReader,
      @Qualifier("circulationLog") FlatFileItemWriter<LogRecord> flatFileItemWriter,
      CsvPartStepExecutionListener csvPartStepExecutionListener) {
    return stepBuilderFactory.get("getCirculationLogPartStep").<LogRecord, LogRecord>chunk(100).reader(circulationLogCsvItemReader)
        .writer(flatFileItemWriter)
        .faultTolerant()
        .allowStartIfComplete(false)
        .throttleLimit(NUMBER_OF_CONCURRENT_TASK_EXECUTIONS)
        .listener(csvPartStepExecutionListener)
        .build();
  }

  @Bean
  @StepScope
  public CirculationLogCsvItemReader reader(@Value("#{jobParameters['query']}") String query,
      @Value("#{stepExecutionContext[offset]}") Long offset, @Value("#{stepExecutionContext[limit]}") Long limit) {
    return new CirculationLogCsvItemReader(auditClient, query, offset, limit);
  }

  @Bean("circulationLog")
  @StepScope
  public FlatFileItemWriter<LogRecord> writer(@Value("#{stepExecutionContext['tempOutputFilePath']}") String tempOutputFilePath) {
    return new CsvWriter<>(tempOutputFilePath,
        new String[] { "userBarcode", "items", "object", "action", "date", "servicePointId", "source", "description" },
        (field, i) -> {
          if (i != 1) {
            return field;
          }
          return ((List<LogRecordItems>) field).stream()
              .filter(lri -> lri != null && StringUtils.isNotBlank(lri.getItemBarcode()))
              .map(LogRecordItems::getItemBarcode)
              .collect(Collectors.joining(","));
        });
  }

}
