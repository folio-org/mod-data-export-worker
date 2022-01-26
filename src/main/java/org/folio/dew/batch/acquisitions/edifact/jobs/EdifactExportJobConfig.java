package org.folio.dew.batch.acquisitions.edifact.jobs;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.batch.CsvFileAssembler;
import org.folio.dew.batch.CsvPartStepExecutionListener;
import org.folio.dew.batch.JobCompletionNotificationListener;
import org.folio.dew.batch.bulkedit.jobs.BulkEditUserProcessor;
import org.folio.dew.batch.bulkedit.jobs.processquery.BulkEditCqlItemReader;
import org.folio.dew.batch.bulkedit.jobs.processquery.BulkEditCqlPartitioner;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.UserFormat;
import org.folio.dew.repository.MinIOObjectStorageRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;

@Configuration
@Log4j2
@RequiredArgsConstructor
public class EdifactExportJobConfig {
  @Autowired
  private final JobBuilderFactory edifactExportJobBuilderFactory;
  @Autowired
  private final StepBuilderFactory edifactExportStepBuilderFactory;
  //private final MinIOObjectStorageRepository repository;


  @Bean
  public Job edifactExportCqlJob(
/*    JobCompletionNotificationListener jobCompletionNotificationListener,
    Step bulkEditCqlStep,*/
    JobRepository jobRepository) {
    return edifactExportJobBuilderFactory
      .get(ExportType.EDIFACT_ORDERS_EXPORT.toString())
      .repository(jobRepository)
      .incrementer(new RunIdIncrementer())
      //.listener(jobCompletionNotificationListener)
      .flow(mapToEdifactStep())
      .end()
      .build();
  }

  @Bean public Step mapToEdifactStep() {
    return edifactExportStepBuilderFactory.get("mapToEdifact").tasklet(mapToEdifactTasklet()).build();

  }

  @Bean public Tasklet mapToEdifactTasklet() {
    return (a, b) -> {
      return RepeatStatus.FINISHED;
    };
  }

  @Bean
  public Step saveToMinIO() {
    return edifactExportStepBuilderFactory.get("saveToMinIO").tasklet(mapToEdifactTasklet()).build();
  }

  @Bean
  public Step saveToSFTP() {
    return edifactExportStepBuilderFactory.get("saveToSFTP").tasklet(mapToEdifactTasklet()).build();
  }

  @Bean
  public Step createExportHistoryRecords() {
    return edifactExportStepBuilderFactory.get("createExportHistoryRecords").tasklet(mapToEdifactTasklet()).build();
  }

/*  @Bean
  public Step edifactExportCqlPartitionStep(
    BulkEditCqlItemReader bulkEditCqlItemReader,
    FlatFileItemWriter<UserFormat> writer,
    BulkEditUserProcessor processor,
    CsvPartStepExecutionListener csvPartStepExecutionListener
  ) {
    return stepBuilderFactory
      .get("bulkEditCqlPartitionStep")
      .<User, UserFormat>chunk(100)
      .reader(bulkEditCqlItemReader)
      .processor(processor)
      .writer(writer)
      .faultTolerant()
      .allowStartIfComplete(false)
      .throttleLimit(10)
      .listener(csvPartStepExecutionListener)
      .build();
  }*/
/*
  @Bean
  public Step edifactExportCqlStep(
    Step edifactExportCqlPartitionStep,
    EdifactExportCqlPartitioner partitioner,
    TaskExecutor asyncTaskExecutor,
    CsvFileAssembler csvFileAssembler) {
    return stepBuilderFactory
      .get("edifactExportCqlChunkStep")
      .partitioner("edifactExportCqlPartitionStep", partitioner)
      .taskExecutor(asyncTaskExecutor)
      .step(edifactExportCqlPartitionStep)
      .aggregator(csvFileAssembler)
      .build();
  }


  @Bean
  public Step saveToMinioStep(
    Step edifactExportCqlPartitionStep,
    EdifactExportCqlPartitioner partitioner,
    TaskExecutor asyncTaskExecutor,
    CsvFileAssembler csvFileAssembler) {
    return stepBuilderFactory
      .get("saveFileToMinioStep")
      .partitioner("edifactExportCqlPartitionStep", partitioner)
      .taskExecutor(asyncTaskExecutor)
      .step(edifactExportCqlPartitionStep)
      .aggregator(csvFileAssembler)
      .build();
  }


  @Bean
  @StepScope
  public EdifactExportCqlPartitioner edifactExportCqlPartitioner(
    @Value("#{jobParameters['offset']}") Long offset,
    @Value("#{jobParameters['limit']}") Long limit,
    @Value("#{jobParameters['tempOutputFilePath']}") String tempOutputFilePath,
    @Value("#{jobParameters['query']}") String query) {
    return new EdifactExportCqlPartitioner(offset, limit, query);
  }*/


}
