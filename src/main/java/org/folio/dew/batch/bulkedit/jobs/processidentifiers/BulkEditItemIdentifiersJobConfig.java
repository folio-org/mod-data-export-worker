package org.folio.dew.batch.bulkedit.jobs.processidentifiers;

import static org.folio.dew.domain.dto.EntityType.ITEM;
import static org.folio.dew.domain.dto.JobParameterNames.TEMP_LOCAL_FILE_PATH;
import static org.folio.dew.domain.dto.JobParameterNames.TEMP_OUTPUT_CSV_PATH;
import static org.folio.dew.domain.dto.JobParameterNames.TEMP_OUTPUT_JSON_PATH;
import static org.folio.dew.utils.Constants.JOB_NAME_POSTFIX_SEPARATOR;
import static org.folio.dew.utils.Constants.TEMP_IDENTIFIERS_FILE_NAME;

import lombok.RequiredArgsConstructor;
import org.folio.dew.batch.CsvListFileWriter;
import org.folio.dew.batch.JsonListFileWriter;
import org.folio.dew.batch.JobCompletionNotificationListener;
import org.folio.dew.batch.bulkedit.jobs.BulkEditItemListProcessor;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.ItemFormat;
import org.folio.dew.domain.dto.ItemIdentifier;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.error.BulkEditSkipListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class BulkEditItemIdentifiersJobConfig {
  private final BulkEditItemListProcessor bulkEditItemListProcessor;
  private final ItemFetcher itemFetcher;
  private final BulkEditSkipListener bulkEditSkipListener;

  @Value("${application.chunks}")
  private int chunks;

  @Value("${application.num-partitions}")
  private int numPartitions;

  @Bean
  public Job bulkEditProcessItemIdentifiersJob(JobCompletionNotificationListener listener, Step itemPartitionStep,
                                               JobRepository jobRepository) {
    return new JobBuilder(ExportType.BULK_EDIT_IDENTIFIERS + JOB_NAME_POSTFIX_SEPARATOR + ITEM.getValue(), jobRepository)
      .incrementer(new RunIdIncrementer())
      .listener(listener)
      .flow(itemPartitionStep)
      .end()
      .build();
  }

  @Bean
  public Step itemPartitionStep(FlatFileItemReader<ItemIdentifier> csvItemIdentifierReader,
                                CompositeItemWriter<List<ItemFormat>> compositeItemListWriter,
                                ListIdentifiersWriteListener<ItemFormat> listIdentifiersWriteListener, JobRepository jobRepository,
                                PlatformTransactionManager transactionManager,
                                @Qualifier("asyncTaskExecutorBulkEdit") TaskExecutor taskExecutor,
                                Partitioner bulkEditItemPartitioner, BulkEditFileAssembler bulkEditFileAssembler) {
    return new StepBuilder("itemPartitionStep", jobRepository)
      .partitioner("bulkEditItemStep", bulkEditItemPartitioner)
      .gridSize(numPartitions)
      .step(bulkEditItemStep(csvItemIdentifierReader, compositeItemListWriter, listIdentifiersWriteListener, jobRepository,
        transactionManager))
      .taskExecutor(taskExecutor)
      .aggregator(bulkEditFileAssembler)
      .build();
  }

  @Bean
  @StepScope
  public Partitioner bulkEditItemPartitioner(@Value("#{jobParameters['" + TEMP_LOCAL_FILE_PATH + "']}") String outputCsvJsonFilePath,
                                             @Value("#{jobParameters['" + TEMP_IDENTIFIERS_FILE_NAME + "']}") String uploadedFileName) {
    return new BulkEditPartitioner(outputCsvJsonFilePath, outputCsvJsonFilePath, null, uploadedFileName);
  }

  @Bean
  public Step bulkEditItemStep(FlatFileItemReader<ItemIdentifier> csvItemIdentifierReader,
                               CompositeItemWriter<List<ItemFormat>> compositeItemListWriter,
                               ListIdentifiersWriteListener<ItemFormat> listIdentifiersWriteListener, JobRepository jobRepository,
                               PlatformTransactionManager transactionManager) {
    return new StepBuilder("bulkEditItemStep", jobRepository)
      .<ItemIdentifier, List<ItemFormat>> chunk(chunks, transactionManager)
      .reader(csvItemIdentifierReader)
      .processor(identifierItemProcessor())
      .faultTolerant()
      .skipLimit(1_000_000)
      .processorNonTransactional() // Required to avoid repeating BulkEditItemProcessor#process after skip.
      .skip(BulkEditException.class)
      .listener(bulkEditSkipListener)
      .writer(compositeItemListWriter)
      .listener(listIdentifiersWriteListener)
      .build();
  }

  @Bean
  public CompositeItemProcessor<ItemIdentifier, List<ItemFormat>> identifierItemProcessor() {
    var processor = new CompositeItemProcessor<ItemIdentifier, List<ItemFormat>>();
    processor.setDelegates(Arrays.asList(itemFetcher, bulkEditItemListProcessor));
    return processor;
  }

  @Bean
  @StepScope
  public CompositeItemWriter<List<ItemFormat>> compositeItemListWriter(
    @Value("#{stepExecutionContext['" + TEMP_OUTPUT_CSV_PATH + "']}") String csvPath,
    @Value("#{stepExecutionContext['" + TEMP_OUTPUT_JSON_PATH + "']}") String jsonPath) {
    var writer = new CompositeItemWriter<List<ItemFormat>>();
    writer.setDelegates(Arrays.asList(
      new CsvListFileWriter<>(csvPath, null, ItemFormat.getItemFieldsArray(), (field, i) -> field),
      new JsonListFileWriter<>(new FileSystemResource(jsonPath))));
    return writer;
  }
}
