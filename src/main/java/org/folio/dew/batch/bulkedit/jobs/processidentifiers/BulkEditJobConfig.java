package org.folio.dew.batch.bulkedit.jobs.processidentifiers;

import lombok.RequiredArgsConstructor;
import org.folio.des.domain.dto.ExportType;
import org.folio.dew.batch.CsvFileAssembler;
import org.folio.dew.batch.CsvPartStepExecutionListener;
import org.folio.dew.batch.CsvWriter;
import org.folio.dew.batch.JobCompletionNotificationListener;
import org.folio.dew.batch.circulationlog.CirculationLogCsvItemReader;
import org.folio.dew.batch.circulationlog.CirculationLogCsvPartitioner;
import org.folio.dew.batch.circulationlog.CirculationLogItemProcessor;
import org.folio.dew.domain.dto.CirculationLogExportFormat;
import org.folio.dew.domain.dto.LogRecord;
import org.folio.dew.domain.dto.UserFormat;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.transform.FixedLengthTokenizer;
import org.springframework.batch.item.file.transform.Range;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.TaskExecutor;

@Configuration
@RequiredArgsConstructor
public class BulkEditJobConfig {
  private static final int NUMBER_OF_CONCURRENT_TASK_EXECUTIONS = 10;

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;

  @Bean
  public Job getBulkEditJob(
    JobCompletionNotificationListener jobCompletionNotificationListener,
    @Qualifier("getBulkEditStep") Step getBulkEditStep,
    JobRepository jobRepository) {
    return jobBuilderFactory
      //TODO change to des value
      .get("BULK-EDIT")
      .repository(jobRepository)
      .incrementer(new RunIdIncrementer())
      .listener(jobCompletionNotificationListener)
      .flow(getBulkEditStep)
      .end()
      .build();
  }

  @Bean("getBulkEditStep")
  public Step getBulkEditStep(
    @Qualifier("getBulkEditPartStep") Step getBulkEditPartStep,
    @Qualifier("getBulkEditPartitioner") Partitioner partitioner,
    @Qualifier("asyncTaskExecutor") TaskExecutor taskExecutor,
    CsvFileAssembler csvFileAssembler) {
    return stepBuilderFactory
      .get("getBulkEditChunkStep")
      .partitioner("getBulkEditPartStep", partitioner)
      .taskExecutor(taskExecutor)
      .step(getBulkEditPartStep)
      .aggregator(csvFileAssembler)
      .build();
  }

  @Bean("getBulkEditPartitioner")
  @StepScope
  public Partitioner getBulkEditPartitioner(
    @Value("#{jobParameters['offset']}") Long offset,
    @Value("#{jobParameters['limit']}") Long limit,
    @Value("#{jobParameters['tempOutputFilePath']}") String tempOutputFilePath,
    @Value("#{jobParameters['query']}") String query) {
    return new CirculationLogCsvPartitioner(offset, limit, tempOutputFilePath, auditClient, query);
  }

  @Bean("getBulkEditPartStep")
  public Step getBulkEditPartStep(
    CirculationLogCsvItemReader circulationLogCsvItemReader,
    @Qualifier("bulkEdit") FlatFileItemWriter<UserFormat> flatFileItemWriter,
    BulkEditItemProcessor bulkEditItemProcessor,
    CsvPartStepExecutionListener csvPartStepExecutionListener) {
    return stepBuilderFactory
      .get("getBulkEditPartStep")
      .<LogRecord, CirculationLogExportFormat>chunk(100)
      .reader(circulationLogCsvItemReader)
      .processor(bulkEditItemProcessor)
      .writer(flatFileItemWriter)
      .faultTolerant()
      .allowStartIfComplete(false)
      .throttleLimit(NUMBER_OF_CONCURRENT_TASK_EXECUTIONS)
      .listener(csvPartStepExecutionListener)
      .build();
  }

  @Bean
  @StepScope
  public BulkEditCsvItemReader reader(
    @Value("#{jobParameters['fileName']}") String fileName,
    @Value("#{stepExecutionContext['offset']}") Long offset,
    @Value("#{stepExecutionContext['limit']}") Long limit) {
    return new BulkEditCsvItemReader(fileName, offset, limit);
  }

  @Bean("bulkEdit")
  @StepScope
  public FlatFileItemWriter<UserFormat> writer(
    @Value("#{stepExecutionContext['tempOutputFilePath']}") String tempOutputFilePath,
    @Value("#{stepExecutionContext['partition']}") Long partition) {
    return new CsvWriter<>(tempOutputFilePath, partition,
      "User name, Id, External system id, Barcode, Active, Type, Patron group, Departments, Proxy for, Last name, First name, Middle name, Preferred first name, Email, Phone, Mobile phone, Date of birth, Addresses, Preferred contact type id, Enrollment date, Expiration date, Created date, Updated date, Tags, Custom fields",
      new String[] { "userName", "id", "externalSystemId", "barcode", "active", "type", "patronGroup", "departments", "proxyFor", "lastName", "firstName", "middleName", "preferredFirstName", "email", "phone", "mobilePhone", "dateOfBirth", "addresses", "preferredContactTypeId", "enrollmentDate", "expirationDate", "createdDate", "updatedDate", "tags", "customFields" },
      (field, i) -> field);
  }
}
