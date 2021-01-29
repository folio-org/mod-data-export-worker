package org.folio.springConfigs;

import com.zaxxer.hikari.HikariDataSource;
import org.folio.dto.GreetingDto;
import org.folio.model.*;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;

@Configuration
@EnableBatchProcessing
@EnableAsync
public class BatchConfiguration {

    private final static int NUMBER_OF_CONCURRENT_TASK_EXECUTIONS = 10;

    private final static int MAX_DATA_SOURCE_POOL_SIZE = 20;

    private final static int TASK_EXECUTOR_CORE_POOL_SIZE = 10;

    private final static int TASK_EXECUTOR_MAX_POOL_SIZE = 10;

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    @StepScope
    public RestApiGreetingItemReader reader(
            @Value("#{stepExecutionContext[greetingsOffset]}") Long offset,
            @Value("#{stepExecutionContext[greetingsLimit]}") Long limit) {

        // TODO move it out to application.properties
        final String getGreetingsMethodUrlTemplate = "http://localhost:8083/greeting/search?offset=%d&limit=%d";

        RestTemplate restTemplate = new RestTemplate();
        int offsetInt = offset.intValue();
        int limitInt = limit.intValue();

        RestApiGreetingItemReader restApiGreetingItemReader = new RestApiGreetingItemReader(getGreetingsMethodUrlTemplate, restTemplate, offsetInt, limitInt);
        return restApiGreetingItemReader;
    }

    @Bean
    @StepScope
    public FlatFileItemWriter<GreetingDto> writer(@Value("#{stepExecutionContext['outputFilePath']}") String outputFilePath) {
        final String commaDelimiter = ",";

        // TODO validate input parameters there

        if (outputFilePath == null) {
            return null;
        }

        Resource outputFile = new FileSystemResource(outputFilePath);

        FlatFileItemWriter<GreetingDto> flatFileItemWriter = new FlatFileItemWriter<GreetingDto>();
        flatFileItemWriter.setName("greetingsWriter");

        DelimitedLineAggregator<GreetingDto> lineAggregator = new DelimitedLineAggregator<GreetingDto>();
        lineAggregator.setDelimiter(commaDelimiter);

        BeanWrapperFieldExtractor<GreetingDto> fieldExtractor = new BeanWrapperFieldExtractor<GreetingDto>();
        String[] extractedFieldNames = {"id", "greeting", "language"};
        fieldExtractor.setNames(extractedFieldNames);
        lineAggregator.setFieldExtractor(fieldExtractor);

        flatFileItemWriter.setLineAggregator(lineAggregator);
        flatFileItemWriter.setResource(outputFile);

        flatFileItemWriter.setAppendAllowed(true);

        return flatFileItemWriter;
    }

    @Bean
    public Job getGreetingsJob(
            JobCompletionNotificationListener jobCompletionNotificationListener,
            @Qualifier("getGreetingsStep") Step getGreetingsStep,
            JobRepository jobRepository) {
        final String jobName = "getGreetingsJob";

        // TODO validate input parameters here

        Job job = this.jobBuilderFactory.get(jobName)
                .repository(jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(jobCompletionNotificationListener)
                .flow(getGreetingsStep)
                .end()
                .build();
        return job;
    }

    @Bean("getGreetingsStep")
    public Step getGreetingsStep(
            @Qualifier("getGreetingsPartStep") Step getGreetingsPartStep,
            Partitioner partitioner,
            @Qualifier("asyncTaskExecutor") TaskExecutor taskExecutor,
            CsvFileAssembler csvFileAssembler) {
        Step getGreetingsChunkStep = this.stepBuilderFactory.get("getGreetingsChunkStep")
                .partitioner("getGreetingsPartStep", partitioner)
                .taskExecutor(taskExecutor)
                .step(getGreetingsPartStep)
                .aggregator(csvFileAssembler)
                .build();

        return getGreetingsChunkStep;
    }

    @Bean("getGreetingsPartStep")
    public Step getGreetingsPartStep(RestApiGreetingItemReader restApiGreetingItemReader,
                                     FlatFileItemWriter<GreetingDto> flatFileItemWriter,
                                     GreetingsPartStepExecutionListener greetingsPartStepExecutionListener) {
        final int numberOfRetries = 3;
        // TODO Add retry logic here https://docs.spring.io/spring-batch/docs/current/reference/html/index-single.html#retryLogic

        Step getGreetingsPartStep = this.stepBuilderFactory.get("getGreetingsPartStep")
                .<GreetingDto, GreetingDto> chunk(100)
                .reader(restApiGreetingItemReader)
                .writer(flatFileItemWriter)
                .faultTolerant()
                // TODO Uncomment this line, once appropriate exceptions are added below
                // .retryLimit(numberOfRetries)
                // TODO Add exceptions here on which Task execution should be retried
                .allowStartIfComplete(false)
                .throttleLimit(NUMBER_OF_CONCURRENT_TASK_EXECUTIONS)
                .listener(greetingsPartStepExecutionListener)
                .build();
        return getGreetingsPartStep;
    }

    @Bean
    @StepScope
    public Partitioner getGreetingsPartitioner(
            @Value("#{jobParameters['offset']}") Long greetingsOffset,
            @Value("#{jobParameters['limit']}") Long greetingsLimit,
            @Value("#{jobParameters['outputFilePath']}") String outputFilePath) {

        if (greetingsOffset == null
                || greetingsLimit == null
                || outputFilePath == null) {
            return null;
        }

        int greetingsOffsetInt = greetingsOffset.intValue();
        int greetingsLimitInt = greetingsLimit.intValue();
        GetGreetingsPartitioner getGreetingsPartitioner = new GetGreetingsPartitioner(greetingsOffsetInt, greetingsLimitInt, outputFilePath);
        return getGreetingsPartitioner;
    }

    @Bean(name = "asyncJobLauncher")
    public JobLauncher getAsyncJobLauncher(
            JobRepository jobRepository,
            @Qualifier("asyncTaskExecutor") TaskExecutor taskExecutor) {
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
    public JobRepository getJobRepository(
            @Qualifier("jobRepositoryDataSource") DataSource dataSource,
            PlatformTransactionManager transactionManager) throws Exception {
        JobRepositoryFactoryBean jobRepositoryFactory = new JobRepositoryFactoryBean();
        jobRepositoryFactory.setDataSource(dataSource);
        jobRepositoryFactory.setTransactionManager(transactionManager);
        jobRepositoryFactory.setIsolationLevelForCreate("ISOLATION_SERIALIZABLE");
        jobRepositoryFactory.setTablePrefix("BATCH_");
        jobRepositoryFactory.setMaxVarCharLength(1000);

        JobRepository jobRepository = jobRepositoryFactory.getObject();

        return jobRepository;
    }

    @Bean("jobRepositoryDataSource")
    public DataSource getJobRepositoryDataSource(
            @Value("${dataSource.driver}") String driverClassName,
            @Value("${dataSource.jdbcUrl}") String jdbcUrl,
            @Value("${dataSource.username}") String userName,
            @Value("${dataSource.password}") String password) {

        // TODO validate input parameters here

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUsername(userName);
        dataSource.setPassword(password);
        dataSource.setMaximumPoolSize(MAX_DATA_SOURCE_POOL_SIZE);

        return dataSource;
    }
}
