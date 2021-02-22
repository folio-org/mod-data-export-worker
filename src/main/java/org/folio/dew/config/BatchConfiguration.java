package org.folio.dew.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.folio.dew.client.AuditClient;
import org.folio.dew.domain.dto.LogRecord;
import org.folio.dew.model.CsvFileAssembler;
import org.folio.dew.model.JobCompletionNotificationListener;
import org.folio.dew.model.circulationlog.CirculationLogFeignItemReader;
import org.folio.dew.model.circulationlog.CirculationLogPartStepExecutionListener;
import org.folio.dew.model.circulationlog.CirculationLogPartitioner;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
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
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@EnableBatchProcessing
@EnableAsync
@RequiredArgsConstructor
public class BatchConfiguration {

  private static final int NUMBER_OF_CONCURRENT_TASK_EXECUTIONS = 10;
  private static final int MAX_DATA_SOURCE_POOL_SIZE = 20;
  private static final int TASK_EXECUTOR_CORE_POOL_SIZE = 10;
  private static final int TASK_EXECUTOR_MAX_POOL_SIZE = 10;

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

    return flatFileItemWriter;
  }

  @Bean
  public Job getCirculationLogJob(JobCompletionNotificationListener jobCompletionNotificationListener,
      @Qualifier("getCirculationLogStep") Step getCirculationLogStep, JobRepository jobRepository) {
    final String jobName = "getCirculationLogJob";

    return jobBuilderFactory.get(jobName)
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
    // TODO Add retry logic here https://docs.spring.io/spring-batch/docs/current/reference/html/index-single.html#retryLogic

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

  @Bean(name = "asyncJobLauncher")
  public JobLauncher getAsyncJobLauncher(JobRepository jobRepository, @Qualifier("asyncTaskExecutor") TaskExecutor taskExecutor) {
    final SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
    jobLauncher.setJobRepository(jobRepository);
    jobLauncher.setTaskExecutor(taskExecutor);
    return jobLauncher;
  }

  @Bean(name = "asyncTaskExecutor")
  public TaskExecutor getAsyncTaskExecutor() {
    ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
    threadPoolTaskExecutor.setCorePoolSize(TASK_EXECUTOR_CORE_POOL_SIZE);
    threadPoolTaskExecutor.setMaxPoolSize(TASK_EXECUTOR_MAX_POOL_SIZE);
    return threadPoolTaskExecutor;
  }

  @Bean
  public JobRepository getJobRepository(@Qualifier("jobRepositoryDataSource") DataSource dataSource,
      PlatformTransactionManager transactionManager) throws Exception {
    JobRepositoryFactoryBean jobRepositoryFactory = new JobRepositoryFactoryBean();
    jobRepositoryFactory.setDataSource(dataSource);
    jobRepositoryFactory.setTransactionManager(transactionManager);
    jobRepositoryFactory.setIsolationLevelForCreate("ISOLATION_SERIALIZABLE");
    jobRepositoryFactory.setTablePrefix("BATCH_");
    jobRepositoryFactory.setMaxVarCharLength(1000);

    return jobRepositoryFactory.getObject();
  }

  @Bean("jobRepositoryDataSource")
  public DataSource getJobRepositoryDataSource(@Value("${dataSource.driver}") String driverClassName,
      @Value("${dataSource.jdbcUrl}") String jdbcUrl, @Value("${dataSource.username}") String userName,
      @Value("${dataSource.password}") String password) {

    HikariDataSource dataSource = new HikariDataSource();
    dataSource.setDriverClassName(driverClassName);
    dataSource.setJdbcUrl(jdbcUrl);
    dataSource.setUsername(userName);
    dataSource.setPassword(password);
    dataSource.setMaximumPoolSize(MAX_DATA_SOURCE_POOL_SIZE);

    return dataSource;
  }

}
