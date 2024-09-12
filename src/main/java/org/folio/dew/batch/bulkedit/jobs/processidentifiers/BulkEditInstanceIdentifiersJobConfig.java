package org.folio.dew.batch.bulkedit.jobs.processidentifiers;


import lombok.RequiredArgsConstructor;
import org.folio.dew.batch.CsvFileAssembler;
import org.folio.dew.batch.CsvListFileWriter;
import org.folio.dew.batch.JobCompletionNotificationListener;
import org.folio.dew.batch.JsonListFileWriter;
import org.folio.dew.batch.MarcAsListStringsWriter;
import org.folio.dew.batch.bulkedit.jobs.BulkEditInstanceProcessor;
import org.folio.dew.batch.circulationlog.CirculationLogCsvPartitioner;
import org.folio.dew.client.SrsClient;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.InstanceFormat;
import org.folio.dew.domain.dto.ItemIdentifier;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.error.BulkEditSkipListener;
import org.folio.dew.service.JsonToMarcConverter;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.partition.support.MultiResourcePartitioner;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.folio.dew.domain.dto.EntityType.INSTANCE;
import static org.folio.dew.domain.dto.JobParameterNames.TEMP_LOCAL_FILE_PATH;
import static org.folio.dew.domain.dto.JobParameterNames.TEMP_LOCAL_MARC_PATH;
import static org.folio.dew.utils.Constants.CHUNKS;
import static org.folio.dew.utils.Constants.JOB_NAME_POSTFIX_SEPARATOR;
import static org.folio.dew.utils.Constants.TEMP_IDENTIFIERS_FILE_NAME;

@Configuration
@RequiredArgsConstructor
public class BulkEditInstanceIdentifiersJobConfig {
  private final BulkEditInstanceProcessor bulkEditInstanceProcessor;
  private final BulkEditSkipListener bulkEditSkipListener;
  private final SrsClient srsClient;
  private final JsonToMarcConverter jsonToMarcConverter;

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
    PlatformTransactionManager transactionManager
    , @Qualifier("asyncTaskExecutor") TaskExecutor taskExecutor
  ) {
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
      .taskExecutor(taskExecutor)
      .build();
  }

  @Bean
  public Step bulkEditInstancePartitionStep(JobRepository jobRepository, Partitioner bulkEditMultiFilesPartitioner,
                                            @Qualifier("asyncTaskExecutor") TaskExecutor taskExecutor, Step bulkEditInstanceStep) {
    return new StepBuilder("bulkEditInstancePartitionStep", jobRepository)
      .partitioner("bulkEditInstanceStep", bulkEditMultiFilesPartitioner)
      .step(bulkEditInstanceStep)
      .gridSize(4)
      .taskExecutor(taskExecutor)
      .build();
  }

  @Bean
  @StepScope
  public Partitioner bulkEditMultiFilesPartitioner(@Value("#{jobParameters['" + TEMP_IDENTIFIERS_FILE_NAME + "']}") String uploadedFileName,
                                                   @Value("#{jobParameters['jobId']}") String jobId) throws IOException {
    MultiResourcePartitioner partitioner = new MultiResourcePartitioner();
    ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(this.getClass().getClassLoader());
    Resource[] resources = resolver.getResources("file:" + splitIntoSmallerFilesAndReturnPath(uploadedFileName, jobId) + "/"+"*.csv");
    partitioner.setResources(resources);
    return partitioner;
  }

  private String splitIntoSmallerFilesAndReturnPath(String uploadedFileName, String jobId) throws IOException {
    var lines = Files.readAllLines(Path.of(uploadedFileName));
    Files.createDirectory(Path.of(jobId));
    final var num_lines_in_file = 2_000;
    var sb = new StringBuilder();
    var from = 1;
    var i = 0;
    for (; i < lines.size(); i++) {
      if (i > 0 && i % num_lines_in_file == 0) {
        Files.write(Path.of(jobId + "/file_from_" + from + "_to_" + i + ".csv"), sb.toString().getBytes());
        sb.setLength(0);
        from = i + 1;
      }
      sb.append(lines.get(i)).append(System.lineSeparator());
    }
    if (!sb.isEmpty()) {
      Files.write(Path.of(jobId + "/file_from_" + from + "_to_" + i + ".csv"), sb.toString().getBytes());
    }
    return jobId;
  }

  @Bean
  @StepScope
  public CompositeItemWriter<List<InstanceFormat>> compositeInstanceListWriter(@Value("#{jobParameters['" + TEMP_LOCAL_FILE_PATH + "']}") String outputFileName,
                                                                               @Value("#{jobParameters['" + TEMP_LOCAL_MARC_PATH + "']}") String outputMarcName) {
    var writer = new CompositeItemWriter<List<InstanceFormat>>();
    writer.setDelegates(Arrays.asList(new CsvListFileWriter<>(outputFileName, InstanceFormat.getInstanceColumnHeaders(), InstanceFormat.getInstanceFieldsArray(), (field, i) -> field),
      new JsonListFileWriter<>(new FileSystemResource(outputFileName + ".json")), new MarcAsListStringsWriter<>(outputMarcName, srsClient, jsonToMarcConverter)));
    return writer;
  }

//  @Bean
//  public org.springframework.batch.core.scope.StepScope stepScope() {
//    org.springframework.batch.core.scope.StepScope stepScope = new org.springframework.batch.core.scope.StepScope();
//    stepScope.setAutoProxy(true);
//    return stepScope;
//  }
}
