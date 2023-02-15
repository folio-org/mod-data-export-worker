package org.folio.dew.batch.bulkedit.jobs.updatejob;

import org.folio.dew.batch.JobCompletionNotificationListener;
import org.folio.dew.batch.bulkedit.jobs.JobConfigReaderHelper;
import org.folio.dew.domain.dto.HoldingsFormat;
import org.folio.dew.domain.dto.HoldingsRecord;
import org.folio.dew.repository.RemoteFilesStorage;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.InputStreamResource;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;

import static org.folio.dew.domain.dto.EntityType.HOLDINGS_RECORD;
import static org.folio.dew.domain.dto.ExportType.BULK_EDIT_UPDATE;
import static org.folio.dew.domain.dto.JobParameterNames.UPDATED_FILE_NAME;
import static org.folio.dew.utils.Constants.JOB_NAME_POSTFIX_SEPARATOR;

@Configuration public class BulkEditUpdateHoldingsRecordsJobConfig {

  @Bean public Job bulkEditUpdateHoldingsRecordsJob(Step bulkEditUpdateHoldingsRecordsStep, JobRepository jobRepository,
    JobCompletionNotificationListener completionListener) {
    return new JobBuilder(BULK_EDIT_UPDATE.getValue() + JOB_NAME_POSTFIX_SEPARATOR + HOLDINGS_RECORD.getValue(), jobRepository)
      .incrementer(new RunIdIncrementer())
      .listener(completionListener)
      .flow(bulkEditUpdateHoldingsRecordsStep)
      .end()
      .build();
  }

  @Bean public Step bulkEditUpdateHoldingsRecordsStep(ItemReader<HoldingsFormat> csvHoldingsRecordsReader,
    @Qualifier("bulkEditUpdateHoldingsRecordsProcessor") ItemProcessor<HoldingsFormat, HoldingsRecord> processor,
    @Qualifier("updateHoldingsRecordsWriter") ItemWriter<HoldingsRecord> writer,
    @Qualifier("updateRecordWriteListener") ItemWriteListener<HoldingsRecord> updateRecordWriteListener,
    JobRepository jobRepository, PlatformTransactionManager transactionManager) {
    return new StepBuilder("bulkEditUpdateHoldingsRecordsStep", jobRepository)
      .<HoldingsFormat, HoldingsRecord>chunk(10, transactionManager)
      .reader(csvHoldingsRecordsReader)
      .processor(processor)
      .writer(writer)
      .listener(updateRecordWriteListener)
      .build();
  }

  @Bean
  @StepScope
  public FlatFileItemReader<HoldingsFormat> csvHoldingsRecordsReader(
    @Value("#{jobParameters['" + UPDATED_FILE_NAME + "']}") String updatedFileName,
    RemoteFilesStorage remoteFilesStorage)
    throws IOException {
    var holdingsLineMapper = JobConfigReaderHelper.createLineMapper(HoldingsFormat.class, HoldingsFormat.getHoldingsFieldsArray());
    return new FlatFileItemReaderBuilder<HoldingsFormat>().name("holdingsReader")
      .resource(new InputStreamResource(remoteFilesStorage.newInputStream(updatedFileName)))
      .linesToSkip(1)
      .lineMapper(holdingsLineMapper)
      .build();
  }
}
