package org.folio.dew.batch.bulkedit.jobs.processidentifiers;

import static org.folio.dew.domain.dto.EntityType.ITEM;
import static org.folio.dew.utils.Constants.CHUNKS;
import static org.folio.dew.utils.Constants.JOB_NAME_POSTFIX_SEPARATOR;

import lombok.RequiredArgsConstructor;
import org.folio.dew.batch.AbstractStorageStreamWriter;
import org.folio.dew.batch.CsvListWriter;
import org.folio.dew.batch.JobCompletionNotificationListener;
import org.folio.dew.batch.bulkedit.jobs.BulkEditItemListProcessor;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.ItemFormat;
import org.folio.dew.domain.dto.ItemIdentifier;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.error.BulkEditSkipListener;
import org.folio.dew.repository.LocalFilesStorage;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class BulkEditItemIdentifiersJobConfig {
  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;
  private final BulkEditItemListProcessor bulkEditItemListProcessor;
  private final ItemFetcher itemFetcher;
  private final BulkEditSkipListener bulkEditSkipListener;
  private final LocalFilesStorage localFilesStorage;

  @Bean
  @StepScope
  public AbstractStorageStreamWriter<List<ItemFormat>, LocalFilesStorage> csvListWriter(
    @Value("#{jobParameters['tempOutputFilePath']}") String outputFileName) {
    return new CsvListWriter<>(outputFileName, ItemFormat.getItemColumnHeaders(), ItemFormat.getItemFieldsArray(), (field, i) -> field, localFilesStorage, true);
  }

  @Bean
  public Job bulkEditProcessItemIdentifiersJob(JobCompletionNotificationListener listener, Step bulkEditItemStep) {
    return jobBuilderFactory
      .get(ExportType.BULK_EDIT_IDENTIFIERS + JOB_NAME_POSTFIX_SEPARATOR + ITEM.getValue())
      .incrementer(new RunIdIncrementer())
      .listener(listener)
      .flow(bulkEditItemStep)
      .end()
      .build();
  }

  @Bean
  public Step bulkEditItemStep(FlatFileItemReader<ItemIdentifier> csvItemIdentifierReader,
    AbstractStorageStreamWriter<List<ItemFormat>, LocalFilesStorage> csvListWriter,
    ListIdentifiersWriteListener<ItemFormat> listIdentifiersWriteListener) {
    return stepBuilderFactory.get("bulkEditItemStep")
      .<ItemIdentifier, List<ItemFormat>> chunk(CHUNKS)
      .reader(csvItemIdentifierReader)
      .processor(identifierItemProcessor())
      .faultTolerant()
      .skipLimit(1_000_000)
      .processorNonTransactional() // Required to avoid repeating BulkEditItemProcessor#process after skip.
      .skip(BulkEditException.class)
      .listener(bulkEditSkipListener)
      .writer(csvListWriter)
      .listener(listIdentifiersWriteListener)
      .build();
  }

  @Bean
  public CompositeItemProcessor<ItemIdentifier, List<ItemFormat>> identifierItemProcessor() {
    var processor = new CompositeItemProcessor<ItemIdentifier, List<ItemFormat>>();
    processor.setDelegates(Arrays.asList(itemFetcher, bulkEditItemListProcessor));
    return processor;
  }
}
