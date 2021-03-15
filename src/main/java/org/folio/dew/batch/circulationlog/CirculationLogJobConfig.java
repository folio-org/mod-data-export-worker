package org.folio.dew.batch.circulationlog;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.des.domain.dto.ExportType;
import org.folio.dew.batch.CsvFileAssembler;
import org.folio.dew.batch.CsvPartStepExecutionListener;
import org.folio.dew.batch.CsvPartitioner;
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
      @Value("#{jobParameters['limit']}") Long limit, @Value("#{jobParameters['tempOutputFilePath']}") String tempOutputFilePath) {
    if (offset == null || limit == null || StringUtils.isBlank(tempOutputFilePath)) {
      throw new IllegalArgumentException(
          String.format("offset %d limit %d tempOutputFilePath %s", offset, limit, tempOutputFilePath));
    }
    return new CsvPartitioner(offset.intValue(), limit.intValue(), tempOutputFilePath);
  }

  @Bean("getCirculationLogPartStep")
  public Step getCirculationLogPartStep(CirculationLogFeignItemReader circulationLogFeignItemReader,
      FlatFileItemWriter<LogRecord> flatFileItemWriter, CsvPartStepExecutionListener csvPartStepExecutionListener) {
    return stepBuilderFactory.get("getCirculationLogPartStep").<LogRecord, LogRecord>chunk(100).reader(
        circulationLogFeignItemReader)
        .writer(flatFileItemWriter)
        .faultTolerant()
        .allowStartIfComplete(false)
        .throttleLimit(NUMBER_OF_CONCURRENT_TASK_EXECUTIONS)
        .listener(csvPartStepExecutionListener)
        .build();
  }

  @Bean
  @StepScope
  public CirculationLogFeignItemReader reader(@Value("#{stepExecutionContext[offset]}") Long offset,
      @Value("#{stepExecutionContext[limit]}") Long limit) {
    int offsetInt = offset.intValue();
    int limitInt = limit.intValue();

    return new CirculationLogFeignItemReader(auditClient, offsetInt, limitInt);
  }

  @Bean
  @StepScope
  public FlatFileItemWriter<LogRecord> writer(@Value("#{stepExecutionContext['tempOutputFilePath']}") String tempOutputFilePath) {
    final String commaDelimiter = ",";

    if (tempOutputFilePath == null) {
      return null;
    }

    Resource outputFile = new FileSystemResource(tempOutputFilePath);

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
    log.info("Creating file {}.", tempOutputFilePath);
    return flatFileItemWriter;
  }

}
