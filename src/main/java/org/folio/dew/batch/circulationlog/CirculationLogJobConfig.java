package org.folio.dew.batch.circulationlog;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.des.domain.dto.ExportType;
import org.folio.dew.batch.CsvFileAssembler;
import org.folio.dew.batch.CsvPartStepExecutionListener;
import org.folio.dew.batch.JobCompletionNotificationListener;
import org.folio.dew.client.AuditClient;
import org.folio.dew.domain.dto.LogRecord;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.TaskExecutor;

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
    if (StringUtils.isBlank(tempOutputFilePath)) {
      throw new IllegalArgumentException("#{stepExecutionContext['tempOutputFilePath']} is blank");
    }

    FlatFileItemWriter<LogRecord> flatFileItemWriter = new FlatFileItemWriter<>();
    flatFileItemWriter.setName("circulationLogWriter");

    DelimitedLineAggregator<LogRecord> lineAggregator = new DelimitedLineAggregator<>();
    lineAggregator.setDelimiter(",");

    BeanWrapperFieldExtractor<LogRecord> fieldExtractor = new BeanWrapperFieldExtractor<>();
    String[] extractedFieldNames = { "id", "eventId", "userBarcode", "items", "object", "action", "date", "servicePointId",
        "source", "description", "linkToIds" };
    fieldExtractor.setNames(extractedFieldNames);
    lineAggregator.setFieldExtractor(fieldExtractor);

    flatFileItemWriter.setLineAggregator(lineAggregator);
    flatFileItemWriter.setResource(new FileSystemResource(tempOutputFilePath));

    flatFileItemWriter.setAppendAllowed(true);
    log.info("Creating file {}.", tempOutputFilePath);
    return flatFileItemWriter;
  }

}
