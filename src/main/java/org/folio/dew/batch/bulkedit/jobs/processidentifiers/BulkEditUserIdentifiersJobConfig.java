package org.folio.dew.batch.bulkedit.jobs.processidentifiers;

import static org.folio.dew.domain.dto.EntityType.USER;
import static org.folio.dew.domain.dto.JobParameterNames.TEMP_LOCAL_FILE_PATH;
import static org.folio.dew.domain.dto.UserFormat.getUserColumnHeaders;
import static org.folio.dew.domain.dto.UserFormat.getUserFieldsArray;
import static org.folio.dew.utils.Constants.CHUNKS;
import static org.folio.dew.utils.Constants.JOB_NAME_POSTFIX_SEPARATOR;

import lombok.RequiredArgsConstructor;
import org.folio.dew.batch.CsvFileWriter;
import org.folio.dew.batch.JsonFileWriter;
import org.folio.dew.batch.JobCompletionNotificationListener;
import org.folio.dew.batch.bulkedit.jobs.BulkEditUserProcessor;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.ItemIdentifier;
import org.folio.dew.domain.dto.UserFormat;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.error.BulkEditSkipListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
public class BulkEditUserIdentifiersJobConfig {
  private final BulkEditUserProcessor bulkEditUserProcessor;
  private final UserFetcher userFetcher;
  private final BulkEditSkipListener bulkEditSkipListener;

  @Bean
  public Job bulkEditProcessUserIdentifiersJob(JobCompletionNotificationListener listener, Step bulkEditUserStep,
                                               JobRepository jobRepository) {
    return new JobBuilder(ExportType.BULK_EDIT_IDENTIFIERS + JOB_NAME_POSTFIX_SEPARATOR + USER.getValue(), jobRepository)
      .incrementer(new RunIdIncrementer())
      .listener(listener)
      .flow(bulkEditUserStep)
      .end()
      .build();
  }

  @Bean
  public Step bulkEditUserStep(FlatFileItemReader<ItemIdentifier> csvItemIdentifierReader,
      CompositeItemWriter<UserFormat> compositeItemWriter,
      IdentifiersWriteListener<UserFormat> identifiersWriteListener, JobRepository jobRepository,
      PlatformTransactionManager transactionManager) {
    return new StepBuilder("bulkEditUserStep", jobRepository)
      .<ItemIdentifier, UserFormat> chunk(CHUNKS, transactionManager)
      .reader(csvItemIdentifierReader)
      .processor(identifierUserProcessor())
      .faultTolerant()
      .skipLimit(1_000_000)
      .processorNonTransactional() // Required to avoid repeating BulkEditItemProcessor#process after skip.
      .skip(BulkEditException.class)
      .listener(bulkEditSkipListener)
      .writer(compositeItemWriter)
      .listener(identifiersWriteListener)
      .build();
  }

  @Bean
  public CompositeItemProcessor<ItemIdentifier, UserFormat> identifierUserProcessor() {
    var processor = new CompositeItemProcessor<ItemIdentifier, UserFormat>();
    processor.setDelegates(Arrays.asList(userFetcher, bulkEditUserProcessor));
    return processor;
  }

  @Bean
  @StepScope
  public CompositeItemWriter<UserFormat> compositeItemWriter(@Value("#{jobParameters['" + TEMP_LOCAL_FILE_PATH +"']}") String outputFileName) {
    var writer = new CompositeItemWriter<UserFormat>();
    writer.setDelegates(Arrays.asList(new CsvFileWriter<>(outputFileName, getUserColumnHeaders(), getUserFieldsArray(), (field, i) -> field),
      new JsonFileWriter<>(new FileSystemResource(outputFileName + ".json"))));
    return writer;
  }
}
