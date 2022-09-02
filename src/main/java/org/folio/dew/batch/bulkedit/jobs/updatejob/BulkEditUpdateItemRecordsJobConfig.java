package org.folio.dew.batch.bulkedit.jobs.updatejob;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.folio.dew.domain.dto.EntityType.ITEM;
import static org.folio.dew.domain.dto.ExportType.BULK_EDIT_UPDATE;
import static org.folio.dew.domain.dto.JobParameterNames.UPDATED_FILE_NAME;
import static org.folio.dew.utils.Constants.FILE_NAME;
import static org.folio.dew.utils.Constants.JOB_NAME_POSTFIX_SEPARATOR;

import org.folio.dew.batch.JobCompletionNotificationListener;
import org.folio.dew.batch.bulkedit.jobs.JobConfigReaderHelper;
import org.folio.dew.domain.dto.Item;
import org.folio.dew.domain.dto.ItemFormat;
import org.folio.dew.repository.LocalFilesStorage;
import org.folio.dew.repository.S3CompatibleResource;
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
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration public class BulkEditUpdateItemRecordsJobConfig {

  @Bean public Job bulkEditUpdateItemRecordsJob(Step bulkEditUpdateItemRecordsStep, JobBuilderFactory jobBuilderFactory,
    JobCompletionNotificationListener completionListener) {
    return jobBuilderFactory.get(BULK_EDIT_UPDATE.getValue() + JOB_NAME_POSTFIX_SEPARATOR + ITEM.getValue())
      .incrementer(new RunIdIncrementer())
      .listener(completionListener)
      .flow(bulkEditUpdateItemRecordsStep)
      .end()
      .build();
  }

  @Bean public Step bulkEditUpdateItemRecordsStep(ItemReader<ItemFormat> csvItemRecordsReader,
    @Qualifier("bulkEditUpdateItemRecordsProcessor") ItemProcessor<ItemFormat, Item> processor,
    @Qualifier("updateItemRecordsWriter") ItemWriter<Item> writer,
    @Qualifier("updateRecordWriteListener") ItemWriteListener<Item> updateRecordWriteListener,
    StepBuilderFactory stepBuilderFactory) {
    return stepBuilderFactory.get("bulkEditUpdateRecordsStep")
      .<ItemFormat, Item>chunk(10)
      .reader(csvItemRecordsReader)
      .processor(processor)
      .writer(writer)
      .listener(updateRecordWriteListener)
      .build();
  }

  @Bean @StepScope public FlatFileItemReader<ItemFormat> csvItemRecordsReader(
    @Value("#{jobParameters['" + FILE_NAME + "']}") String fileName,
    @Value("#{jobParameters['" + UPDATED_FILE_NAME + "']}") String updatedFileName,
    LocalFilesStorage localFilesStorage) {
    LineMapper<ItemFormat> itemLineMapper = JobConfigReaderHelper.createItemLineMapper();
    return new FlatFileItemReaderBuilder<ItemFormat>().name("itemReader")
      .resource(new S3CompatibleResource<>(isEmpty(updatedFileName) ? fileName : updatedFileName, localFilesStorage))
      .linesToSkip(1)
      .lineMapper(itemLineMapper)
      .build();
  }
}
