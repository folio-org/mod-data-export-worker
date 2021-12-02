package org.folio.dew.batch.bulkedit.jobs.processidentifiers;

import lombok.RequiredArgsConstructor;
import org.folio.dew.batch.CsvWriter;
import org.folio.dew.batch.JobCompletionNotificationListener;
import org.folio.dew.domain.dto.UserFormat;
import org.folio.dew.domain.dto.ItemIdentifier;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

@Configuration
@RequiredArgsConstructor
public class BulkEditJobConfig {
  private final int CHUNK_SIZE = 100;
  private final long NO_PARTITIONS = 0L;

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;
  private final BulkEditItemProcessor bulkEditItemProcessor;

  @Bean
  @StepScope
  public FlatFileItemReader<ItemIdentifier> csvItemIdentifierReader(
    @Value("#{jobParameters['identifiersFileName']}") String uploadedFileName) {
    return new FlatFileItemReaderBuilder<ItemIdentifier>()
      .name("userItemIdentifierReader")
      .resource(new FileSystemResource(uploadedFileName))
      .linesToSkip(0)
      .lineMapper(lineMapper())
      .build();
  }

  @Bean
  public LineMapper<ItemIdentifier> lineMapper() {
    var lineMapper = new DefaultLineMapper<ItemIdentifier>();
    var tokenizer = new DelimitedLineTokenizer();
    tokenizer.setNames("itemId");
    var fieldSetMapper = new BeanWrapperFieldSetMapper<ItemIdentifier>();
    fieldSetMapper.setTargetType(ItemIdentifier.class);
    lineMapper.setLineTokenizer(tokenizer);
    lineMapper.setFieldSetMapper(fieldSetMapper);
    return lineMapper;
  }

  @Bean
  @StepScope
  public FlatFileItemWriter<UserFormat> csvItemWriter(
    @Value("#{jobParameters['outputCsvFileName']}") String outputFileName) {
    return new CsvWriter<>(outputFileName, NO_PARTITIONS,
      "User name, User id, External system id, Barcode, Active, Type, Patron group, Departments, Proxy for, Last name, First name, Middle name, Preferred first name, Email, Phone, Mobile phone, Date of birth, Addresses, Preferred contact type id, Enrollment date, Expiration date, Created date, Updated date, Tags, Custom fields",
      new String[] { "userName", "id", "externalSystemId", "barcode", "active", "type", "patronGroup", "departments", "proxyFor", "lastName", "firstName", "middleName", "preferredFirstName", "email", "phone", "mobilePhone", "dateOfBirth", "addresses", "preferredContactTypeId", "enrollmentDate", "expirationDate", "createdDate", "updatedDate", "tags", "customFields" },
      (field, i) -> field);
  }

  @Bean
  public Job bulkEditJob(JobCompletionNotificationListener listener, Step bulkEditStep) {
    return jobBuilderFactory
      .get("BULK-EDIT-IDENTIFIER")
      .incrementer(new RunIdIncrementer())
      .listener(listener)
      .flow(bulkEditStep)
      .end()
      .build();
  }

  @Bean
  public Step bulkEditStep(FlatFileItemReader<ItemIdentifier> csvItemIdentifierReader, FlatFileItemWriter<UserFormat> csvItemWriter) {
    return stepBuilderFactory.get("bulkEditStep")
      .<ItemIdentifier, UserFormat> chunk(CHUNK_SIZE)
      .reader(csvItemIdentifierReader)
      .processor(bulkEditItemProcessor)
      .writer(csvItemWriter)
      .build();
  }
}
