package org.folio.dew.batch.bulkedit.jobs.processidentifiers;

import static org.folio.dew.domain.dto.EntityType.HOLDINGS_RECORD;
import static org.folio.dew.utils.Constants.CHUNKS;
import static org.folio.dew.utils.Constants.JOB_NAME_POSTFIX_SEPARATOR;

import lombok.RequiredArgsConstructor;
import org.folio.dew.batch.AbstractStorageStreamWriter;
import org.folio.dew.batch.CsvAndJsonListWriter;
import org.folio.dew.batch.JobCompletionNotificationListener;
import org.folio.dew.batch.bulkedit.jobs.BulkEditHoldingsProcessor;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.HoldingsFormat;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class BulkEditHoldingsIdentifiersJobConfig {
  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;
  private final BulkEditHoldingsProcessor bulkEditHoldingsProcessor;
  private final BulkEditSkipListener bulkEditSkipListener;
  private final LocalFilesStorage localFilesStorage;

  @Bean
  @StepScope
  public AbstractStorageStreamWriter<List<HoldingsFormat>, LocalFilesStorage> csvHoldingsListWriter(
    @Value("#{jobParameters['tempOutputFilePath']}") String outputFileName) {
    return new CsvAndJsonListWriter<>(outputFileName, HoldingsFormat.getHoldingsColumnHeaders(), HoldingsFormat.getHoldingsFieldsArray(), (field, i) -> field, localFilesStorage);
  }

  @Bean
  public Job bulkEditProcessHoldingsIdentifiersJob(JobCompletionNotificationListener listener, Step bulkEditHoldingsStep) {
    return jobBuilderFactory
      .get(ExportType.BULK_EDIT_IDENTIFIERS + JOB_NAME_POSTFIX_SEPARATOR + HOLDINGS_RECORD.getValue())
      .incrementer(new RunIdIncrementer())
      .listener(listener)
      .flow(bulkEditHoldingsStep)
      .end()
      .build();
  }

  @Bean
  public Step bulkEditHoldingsStep(FlatFileItemReader<ItemIdentifier> csvItemIdentifierReader,
    AbstractStorageStreamWriter<List<HoldingsFormat>, LocalFilesStorage> csvHoldingsListWriter,
    ListIdentifiersWriteListener<HoldingsFormat> listIdentifiersWriteListener) {
    return stepBuilderFactory.get("bulkEditHoldingsStep")
      .<ItemIdentifier, List<HoldingsFormat>> chunk(CHUNKS)
      .reader(csvItemIdentifierReader)
      .processor(bulkEditHoldingsProcessor)
      .faultTolerant()
      .skipLimit(1_000_000)
      .processorNonTransactional() // Required to avoid repeating BulkEditHoldingsProcessor#process after skip.
      .skip(BulkEditException.class)
      .listener(bulkEditSkipListener)
      .writer(csvHoldingsListWriter)
      .listener(listIdentifiersWriteListener)
      .build();
  }
}
