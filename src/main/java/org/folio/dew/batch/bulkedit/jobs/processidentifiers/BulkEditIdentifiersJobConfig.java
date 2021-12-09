package org.folio.dew.batch.bulkedit.jobs.processidentifiers;

import static org.folio.dew.domain.dto.UserFormat.getUserColumnHeaders;
import static org.folio.dew.domain.dto.UserFormat.getUserFieldsArray;

import lombok.RequiredArgsConstructor;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.batch.CsvWriter;
import org.folio.dew.batch.JobCompletionNotificationListener;
import org.folio.dew.batch.bulkedit.jobs.BulkEditUserProcessor;
import org.folio.dew.domain.dto.UserFormat;
import org.folio.dew.domain.dto.ItemIdentifier;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.error.BulkEditSkipListener;
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
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
public class BulkEditIdentifiersJobConfig {
  private static final int CHUNKS = 100;
  private static final long ZERO = 0L;

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;
  private final BulkEditUserProcessor bulkEditUserProcessor;
  private final UserFetcher userFetcher;
  private final BulkEditSkipListener bulkEditSkipListener;

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
    @Value("#{jobParameters['tempOutputFilePath']}") String outputFileName) {
    return new CsvWriter<>(outputFileName, ZERO, getUserColumnHeaders(), getUserFieldsArray(), (field, i) -> field);
  }

  @Bean
  public Job bulkEditJob(JobCompletionNotificationListener listener, Step bulkEditStep) {
    return jobBuilderFactory
      .get(ExportType.BULK_EDIT_IDENTIFIERS.toString())
      .incrementer(new RunIdIncrementer())
      .listener(listener)
      .flow(bulkEditStep)
      .end()
      .build();
  }

  @Bean
  public Step bulkEditStep(FlatFileItemReader<ItemIdentifier> csvItemIdentifierReader, FlatFileItemWriter<UserFormat> csvItemWriter) {
    return stepBuilderFactory.get("bulkEditStep")
      .<ItemIdentifier, UserFormat> chunk(CHUNKS)
      .reader(csvItemIdentifierReader)
      .processor(identifierUserProcessor())
      .faultTolerant()
      .skipLimit(1_000_000)
      .processorNonTransactional() // Required to avoid repeating BulkEditItemProcessor#process after skip.
      .skip(BulkEditException.class)
      .listener(bulkEditSkipListener)
      .writer(csvItemWriter)
      .build();
  }

  @Bean
  public CompositeItemProcessor<ItemIdentifier, UserFormat> identifierUserProcessor() {
    var processor = new CompositeItemProcessor<ItemIdentifier, UserFormat>();
    processor.setDelegates(Arrays.asList(userFetcher, bulkEditUserProcessor));
    return processor;
  }
}
