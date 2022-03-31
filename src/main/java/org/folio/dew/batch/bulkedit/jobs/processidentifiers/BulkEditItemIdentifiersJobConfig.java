package org.folio.dew.batch.bulkedit.jobs.processidentifiers;

import lombok.RequiredArgsConstructor;
import org.folio.dew.batch.CsvWriter;
import org.folio.dew.batch.JobCompletionNotificationListener;
import org.folio.dew.batch.bulkedit.jobs.BulkEditItemProcessor;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.ItemFormat;
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

import static org.folio.dew.domain.dto.EntityType.ITEM;

@Configuration
@RequiredArgsConstructor
public class BulkEditItemIdentifiersJobConfig {
  private static final int CHUNKS = 100;
  private static final long ZERO = 0L;

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;
  private final BulkEditItemProcessor bulkEditItemProcessor;
  private final ItemFetcher itemFetcher;
  private final BulkEditSkipListener bulkEditSkipListener;

  @Bean
  @StepScope
  public FlatFileItemWriter<ItemFormat> csvItemWriter(
    @Value("#{jobParameters['tempOutputFilePath']}") String outputFileName) {
    return new CsvWriter<>(outputFileName, ZERO, ItemFormat.getItemColumnHeaders(), ItemFormat.getItemFieldsArray(), (field, i) -> field);
  }

  @Bean
  public Job bulkEditProcessItemIdentifiersJob(JobCompletionNotificationListener listener, Step bulkEditItemStep) {
    return jobBuilderFactory
      .get(ExportType.BULK_EDIT_IDENTIFIERS + "-" + ITEM.getValue())
      .incrementer(new RunIdIncrementer())
      .listener(listener)
      .flow(bulkEditItemStep)
      .end()
      .build();
  }

  @Bean
  public Step bulkEditItemStep(FlatFileItemReader<ItemIdentifier> csvItemIdentifierReader, FlatFileItemWriter<ItemFormat> csvItemWriter) {
    return stepBuilderFactory.get("bulkEditItemStep")
      .<ItemIdentifier, ItemFormat> chunk(CHUNKS)
      .reader(csvItemIdentifierReader)
      .processor(identifierItemProcessor())
      .faultTolerant()
      .skipLimit(1_000_000)
      .processorNonTransactional() // Required to avoid repeating BulkEditItemProcessor#process after skip.
      .skip(BulkEditException.class)
      .listener(bulkEditSkipListener)
      .writer(csvItemWriter)
      .build();
  }

  @Bean
  public CompositeItemProcessor<ItemIdentifier, ItemFormat> identifierItemProcessor() {
    var processor = new CompositeItemProcessor<ItemIdentifier, ItemFormat>();
    processor.setDelegates(Arrays.asList(itemFetcher, bulkEditItemProcessor));
    return processor;
  }
}
