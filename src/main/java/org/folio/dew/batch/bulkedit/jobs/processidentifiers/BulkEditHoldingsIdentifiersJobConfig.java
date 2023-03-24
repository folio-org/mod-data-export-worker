package org.folio.dew.batch.bulkedit.jobs.processidentifiers;

import static org.folio.dew.domain.dto.EntityType.HOLDINGS_RECORD;
import static org.folio.dew.domain.dto.JobParameterNames.TEMP_LOCAL_FILE_PATH;
import static org.folio.dew.utils.Constants.CHUNKS;
import static org.folio.dew.utils.Constants.JOB_NAME_POSTFIX_SEPARATOR;

import lombok.RequiredArgsConstructor;
import org.folio.dew.batch.CsvListFileWriter;
import org.folio.dew.batch.JsonListFileWriter;
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
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class BulkEditHoldingsIdentifiersJobConfig {
  private final BulkEditHoldingsProcessor bulkEditHoldingsProcessor;
  private final BulkEditSkipListener bulkEditSkipListener;
  private final LocalFilesStorage localFilesStorage;

  @Bean
  public Job bulkEditProcessHoldingsIdentifiersJob(JobCompletionNotificationListener listener,
                                                   JobRepository jobRepository,
                                                   Step bulkEditHoldingsStep) {
    return new JobBuilder(ExportType.BULK_EDIT_IDENTIFIERS + JOB_NAME_POSTFIX_SEPARATOR + HOLDINGS_RECORD.getValue(), jobRepository)
      .incrementer(new RunIdIncrementer())
      .listener(listener)
      .flow(bulkEditHoldingsStep)
      .end()
      .build();
  }

  @Bean
  public Step bulkEditHoldingsStep(FlatFileItemReader<ItemIdentifier> csvItemIdentifierReader,
    CompositeItemWriter<List<HoldingsFormat>> compositeHoldingsListWriter,
    ListIdentifiersWriteListener<HoldingsFormat> listIdentifiersWriteListener, JobRepository jobRepository,
    PlatformTransactionManager transactionManager) {
    return new StepBuilder("bulkEditHoldingsStep", jobRepository)
      .<ItemIdentifier, List<HoldingsFormat>> chunk(CHUNKS, transactionManager)
      .reader(csvItemIdentifierReader)
      .processor(bulkEditHoldingsProcessor)
      .faultTolerant()
      .skipLimit(1_000_000)
      .processorNonTransactional() // Required to avoid repeating BulkEditHoldingsProcessor#process after skip.
      .skip(BulkEditException.class)
      .listener(bulkEditSkipListener)
      .writer(compositeHoldingsListWriter)
      .listener(listIdentifiersWriteListener)
      .build();
  }

  @Bean
  @StepScope
  public CompositeItemWriter<List<HoldingsFormat>> compositeHoldingsListWriter(@Value("#{jobParameters['" + TEMP_LOCAL_FILE_PATH + "']}") String outputFileName) {
    var writer = new CompositeItemWriter<List<HoldingsFormat>>();
    writer.setDelegates(Arrays.asList(new CsvListFileWriter<>(outputFileName, HoldingsFormat.getHoldingsColumnHeaders(), HoldingsFormat.getHoldingsFieldsArray(), (field, i) -> field),
        new JsonListFileWriter<>(new FileSystemResource(outputFileName + ".json"))));
    return writer;
  }
}
