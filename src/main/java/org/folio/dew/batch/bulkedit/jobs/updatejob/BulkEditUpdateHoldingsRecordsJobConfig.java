package org.folio.dew.batch.bulkedit.jobs.updatejob;

import static org.folio.dew.domain.dto.EntityType.HOLDINGS_RECORD;
import static org.folio.dew.domain.dto.ExportType.BULK_EDIT_UPDATE;
import static org.folio.dew.domain.dto.JobParameterNames.UPDATED_FILE_NAME;
import static org.folio.dew.utils.Constants.JOB_NAME_POSTFIX_SEPARATOR;

import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import org.folio.dew.batch.JobCompletionNotificationListener;
import org.folio.dew.batch.bulkedit.jobs.JobConfigReaderHelper;
import org.folio.dew.domain.dto.HoldingsFormat;
import org.folio.dew.domain.dto.HoldingsRecord;
import org.folio.dew.repository.MinIOObjectStorageRepository;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
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

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Configuration public class BulkEditUpdateHoldingsRecordsJobConfig {

  @Bean public Job bulkEditUpdateHoldingsRecordsJob(Step bulkEditUpdateHoldingsRecordsStep, JobBuilderFactory jobBuilderFactory,
    JobCompletionNotificationListener completionListener) {
    return jobBuilderFactory.get(BULK_EDIT_UPDATE.getValue() + JOB_NAME_POSTFIX_SEPARATOR + HOLDINGS_RECORD.getValue())
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
    StepBuilderFactory stepBuilderFactory) {
    return stepBuilderFactory.get("bulkEditUpdateHoldingsRecordsStep")
      .<HoldingsFormat, HoldingsRecord>chunk(10)
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
    MinIOObjectStorageRepository repository)
    throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException,
    InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
    var holdingsLineMapper = JobConfigReaderHelper.createLineMapper(HoldingsFormat.class, HoldingsFormat.getHoldingsFieldsArray());
    return new FlatFileItemReaderBuilder<HoldingsFormat>().name("holdingsReader")
      .resource(new InputStreamResource(repository.getObject(updatedFileName)))
      .linesToSkip(1)
      .lineMapper(holdingsLineMapper)
      .build();
  }
}
