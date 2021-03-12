package org.folio.dew.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.des.domain.dto.ExportType;
import org.folio.dew.batch.CsvFileAssembler;
import org.folio.dew.batch.JobCompletionNotificationListener;
import org.folio.dew.batch.circulationlog.CirculationLogFeignItemReader;
import org.folio.dew.batch.circulationlog.CirculationLogPartStepExecutionListener;
import org.folio.dew.batch.circulationlog.CirculationLogPartitioner;
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
import org.springframework.core.io.Resource;
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
  @StepScope
  public CirculationLogFeignItemReader reader(@Value("#{stepExecutionContext[circulationLogOffset]}") Long offset,
      @Value("#{stepExecutionContext[circulationLogLimit]}") Long limit) {

    int offsetInt = offset.intValue();
    int limitInt = limit.intValue();

    return new CirculationLogFeignItemReader(auditClient, offsetInt, limitInt);
  }

  @Bean
  @StepScope
  public FlatFileItemWriter<LogRecord> writer(@Value("#{stepExecutionContext['outputFilePath']}") String outputFilePath) {
    final String commaDelimiter = ",";

    if (outputFilePath == null) {
      return null;
    }

    Resource outputFile = new FileSystemResource(outputFilePath);

    FlatFileItemWriter<LogRecord> flatFileItemWriter = new FlatFileItemWriter<>();
    flatFileItemWriter.setName("circulationLogWriter");

    DelimitedLineAggregator<LogRecord> lineAggregator = new DelimitedLineAggregator<>();
    lineAggregator.setDelimiter(commaDelimiter);

    BeanWrapperFieldExtractor<LogRecord> fieldExtractor = new BeanWrapperFieldExtractor<>();
    String[] extractedFieldNames = { "id", "eventId", "userBarcode", "items", "object", "action", "date", "servicePointId",
        "source", "description", "linkToIds" };
    fieldExtractor.setNames(extractedFieldNames);
    lineAggregator.setFieldExtractor(fieldExtractor);

    flatFileItemWriter.setLineAggregator(lineAggregator);
    flatFileItemWriter.setResource(outputFile);

    flatFileItemWriter.setAppendAllowed(true);
    log.info("Creating file {}.", outputFilePath);
    return flatFileItemWriter;
  }

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

  @Bean("getCirculationLogPartStep")
  public Step getCirculationLogPartStep(CirculationLogFeignItemReader circulationLogFeignItemReader,
      FlatFileItemWriter<LogRecord> flatFileItemWriter,
      CirculationLogPartStepExecutionListener circulationLogPartStepExecutionListener) {
    final int numberOfRetries = 3;
    // TODO Add retry logic here
    // https://docs.spring.io/spring-batch/docs/current/reference/html/index-single.html#retryLogic

    return stepBuilderFactory.get("getCirculationLogPartStep").<LogRecord, LogRecord>chunk(100).reader(
        circulationLogFeignItemReader)
        .writer(flatFileItemWriter)
        .faultTolerant()
        // TODO Uncomment this line, once appropriate exceptions are added below
        // .retryLimit(numberOfRetries)
        // TODO Add exceptions here on which Task execution should be retried
        .allowStartIfComplete(false)
        .throttleLimit(NUMBER_OF_CONCURRENT_TASK_EXECUTIONS)
        .listener(circulationLogPartStepExecutionListener)
        .build();
  }

  @Bean
  @StepScope
  public Partitioner getCirculationLogPartitioner(@Value("#{jobParameters['offset']}") Long circulationLogOffset,
      @Value("#{jobParameters['limit']}") Long circulationLogLimit,
      @Value("#{jobParameters['outputFilePath']}") String outputFilePath) {

    if (circulationLogOffset == null || circulationLogLimit == null || outputFilePath == null) {
      return null;
    }

    int circulationLogOffsetInt = circulationLogOffset.intValue();
    int circulationLogLimitInt = circulationLogLimit.intValue();
    return new CirculationLogPartitioner(circulationLogOffsetInt, circulationLogLimitInt, outputFilePath);
  }

}
