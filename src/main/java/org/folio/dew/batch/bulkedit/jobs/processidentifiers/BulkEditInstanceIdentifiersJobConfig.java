package org.folio.dew.batch.bulkedit.jobs.processidentifiers;


import lombok.RequiredArgsConstructor;
import org.folio.dew.batch.CsvListFileWriter;
import org.folio.dew.batch.JobCompletionNotificationListener;
import org.folio.dew.batch.JsonListFileWriter;
import org.folio.dew.batch.bulkedit.jobs.BulkEditInstanceProcessor;
import org.folio.dew.client.SrsClient;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.InstanceFormat;
import org.folio.dew.domain.dto.ItemIdentifier;
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
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Arrays;
import java.util.List;

import static org.folio.dew.domain.dto.EntityType.INSTANCE;
import static org.folio.dew.domain.dto.JobParameterNames.TEMP_LOCAL_FILE_PATH;
import static org.folio.dew.domain.dto.JobParameterNames.TEMP_LOCAL_MARC_PATH;
import static org.folio.dew.utils.Constants.CHUNKS;
import static org.folio.dew.utils.Constants.JOB_NAME_POSTFIX_SEPARATOR;

@Configuration
@RequiredArgsConstructor
public class BulkEditInstanceIdentifiersJobConfig {
  private final BulkEditInstanceProcessor bulkEditInstanceProcessor;
  private final BulkEditSkipListener bulkEditSkipListener;
  private final SrsClient srsClient;

  @Bean
  public Job bulkEditProcessInstanceIdentifiersJob(JobCompletionNotificationListener listener, Step bulkEditInstanceStep,
    JobRepository jobRepository) {
    return new JobBuilder(ExportType.BULK_EDIT_IDENTIFIERS + JOB_NAME_POSTFIX_SEPARATOR + INSTANCE.getValue(), jobRepository)
      .incrementer(new RunIdIncrementer())
      .listener(listener)
      .flow(bulkEditInstanceStep)
      .end()
      .build();
  }

  @Bean
  public Step bulkEditInstanceStep(FlatFileItemReader<ItemIdentifier> csvItemIdentifierReader,
    CompositeItemWriter<List<InstanceFormat>> compositeInstanceListWriter,
    ListIdentifiersWriteListener<InstanceFormat> listIdentifiersWriteListener, JobRepository jobRepository,
    PlatformTransactionManager transactionManager) {
    return new StepBuilder("bulkEditInstanceStep", jobRepository)
      .<ItemIdentifier, List<InstanceFormat>> chunk(CHUNKS, transactionManager)
      .reader(csvItemIdentifierReader)
      .processor(bulkEditInstanceProcessor)
      .faultTolerant()
      .skipLimit(1_000_000)
      .processorNonTransactional() // Required to avoid repeating BulkEditItemProcessor#process after skip.
      .skip(BulkEditException.class)
      .listener(bulkEditSkipListener)
      .writer(compositeInstanceListWriter)
      .listener(listIdentifiersWriteListener)
      .build();
  }

  @Bean
  @StepScope
  public CompositeItemWriter<List<InstanceFormat>> compositeInstanceListWriter(@Value("#{jobParameters['" + TEMP_LOCAL_FILE_PATH + "']}") String outputFileName,
                                                                               @Value("#{jobParameters['" + TEMP_LOCAL_MARC_PATH + "']}") String outputMarcName) {
    var writer = new CompositeItemWriter<List<InstanceFormat>>();
    writer.setDelegates(Arrays.asList(new CsvListFileWriter<>(outputFileName, outputMarcName, srsClient, InstanceFormat.getInstanceColumnHeaders(), InstanceFormat.getInstanceFieldsArray(), (field, i) -> field),
      new JsonListFileWriter<>(new FileSystemResource(outputFileName + ".json"))));
    return writer;
  }
}
