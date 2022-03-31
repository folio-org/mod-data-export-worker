package org.folio.dew.batch.bulkedit.jobs.processidentifiers;

import static org.folio.dew.domain.dto.EntityType.USER;
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
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
public class BulkEditUserIdentifiersJobConfig {
  private static final int CHUNKS = 100;
  private static final long ZERO = 0L;

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;
  private final BulkEditUserProcessor bulkEditUserProcessor;
  private final UserFetcher userFetcher;
  private final BulkEditSkipListener bulkEditSkipListener;

  @Bean
  @StepScope
  public FlatFileItemWriter<UserFormat> csvUserWriter(
    @Value("#{jobParameters['tempOutputFilePath']}") String outputFileName) {
    return new CsvWriter<>(outputFileName, ZERO, getUserColumnHeaders(), getUserFieldsArray(), (field, i) -> field);
  }

  @Bean
  public Job bulkEditProcessUserIdentifiersJob(JobCompletionNotificationListener listener, Step bulkEditUserStep) {
    return jobBuilderFactory
      .get(ExportType.BULK_EDIT_IDENTIFIERS + "-" + USER.getValue())
      .incrementer(new RunIdIncrementer())
      .listener(listener)
      .flow(bulkEditUserStep)
      .end()
      .build();
  }

  @Bean
  public Step bulkEditUserStep(FlatFileItemReader<ItemIdentifier> csvItemIdentifierReader, FlatFileItemWriter<UserFormat> csvUserWriter) {
    return stepBuilderFactory.get("bulkEditUserStep")
      .<ItemIdentifier, UserFormat> chunk(CHUNKS)
      .reader(csvItemIdentifierReader)
      .processor(identifierUserProcessor())
      .faultTolerant()
      .skipLimit(1_000_000)
      .processorNonTransactional() // Required to avoid repeating BulkEditItemProcessor#process after skip.
      .skip(BulkEditException.class)
      .listener(bulkEditSkipListener)
      .writer(csvUserWriter)
      .build();
  }

  @Bean
  public CompositeItemProcessor<ItemIdentifier, UserFormat> identifierUserProcessor() {
    var processor = new CompositeItemProcessor<ItemIdentifier, UserFormat>();
    processor.setDelegates(Arrays.asList(userFetcher, bulkEditUserProcessor));
    return processor;
  }
}
