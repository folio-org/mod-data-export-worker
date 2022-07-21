package org.folio.dew.batch.eholdings;

import static org.folio.dew.batch.eholdings.EHoldingsJobConstants.CONTEXT_MAX_PACKAGE_NOTES_COUNT;
import static org.folio.dew.batch.eholdings.EHoldingsJobConstants.CONTEXT_MAX_TITLE_NOTES_COUNT;
import static org.folio.dew.batch.eholdings.EHoldingsJobConstants.CONTEXT_RESOURCES;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.listener.ExecutionContextPromotionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.batch.item.support.ListItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.folio.dew.batch.JobCompletionNotificationListener;
import org.folio.dew.domain.dto.EHoldingsExportConfig;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.eholdings.EHoldingsResource;
import org.folio.dew.domain.dto.eholdings.EHoldingsResourceExportFormat;

@Log4j2
@Configuration
@RequiredArgsConstructor
public class EHoldingsJobConfig {

  private static final int PROCESSING_RECORD_CHUNK_SIZE = 5;

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;
  private final ObjectMapper objectMapper;

  @Bean
  public Job getEHoldingsJob(
    JobRepository jobRepository,
    JobCompletionNotificationListener jobCompletionNotificationListener,
    @Qualifier("prepareEHoldingsStep") Step prepareEHoldingsStep,
    @Qualifier("getEHoldingsStep") Step getEHoldingsStep,
    @Qualifier("saveEHoldingsStep") Step saveEHoldingsStep) {
    return jobBuilderFactory
      .get(ExportType.E_HOLDINGS.toString())
      .repository(jobRepository)
      .incrementer(new RunIdIncrementer())
      .listener(jobCompletionNotificationListener)
      .start(prepareEHoldingsStep)
      .next(getEHoldingsStep)
      .next(saveEHoldingsStep)
      .build();
  }

  @Bean("prepareEHoldingsStep")
  public Step prepareEHoldingsStep(EHoldingsPreparationTasklet preparationTasklet,
                                   @Qualifier("prepareEHoldingsPromotionListener")
                                     ExecutionContextPromotionListener prepareEHoldingsPromotionListener) {
    return stepBuilderFactory
      .get("prepareEHoldingsStep")
      .tasklet(preparationTasklet)
      .listener(prepareEHoldingsPromotionListener)
      .build();
  }

  @Bean("getEHoldingsStep")
  public Step getEHoldingsStep(@Qualifier("eHoldingsItemProcessor")
                                 ItemProcessor<EHoldingsResource, EHoldingsResource> processor,
                               @Qualifier("getEHoldingsPromotionListener")
                                 ExecutionContextPromotionListener getEHoldingsPromotionListener,
                               EHoldingsItemReader eHoldingsCsvItemReader,
                               ListItemWriter<EHoldingsResource> listItemWriter,
                               EHoldingsNoteItemProcessor eHoldingsNoteItemProcessor) {
    return stepBuilderFactory
      .get("getEHoldingsStep")
      .<EHoldingsResource, EHoldingsResource>chunk(PROCESSING_RECORD_CHUNK_SIZE)
      .reader(eHoldingsCsvItemReader)
      .processor(processor)
      .writer(listItemWriter)
      .listener(eHoldingsNoteItemProcessor)
      .listener(stepExecutionListener(listItemWriter))
      .listener(getEHoldingsPromotionListener)
      .build();
  }

  @Bean("saveEHoldingsStep")
  public Step saveEHoldingsStep(EHoldingsListItemReader eholdingsListItemReader,
                                EHoldingsCsvFileWriter flatFileItemWriter,
                                EHoldingsStepListener eHoldingsStepListener) {
    return stepBuilderFactory
      .get("saveEHoldingsStep")
      .<EHoldingsResourceExportFormat, EHoldingsResourceExportFormat>chunk(PROCESSING_RECORD_CHUNK_SIZE)
      .reader(eholdingsListItemReader)
      .writer(flatFileItemWriter)
      .listener(eHoldingsStepListener)
      .build();
  }

  @StepScope
  @Bean("prepareEHoldingsPromotionListener")
  public ExecutionContextPromotionListener prepareEHoldingsPromotionListener() {
    var listener = new ExecutionContextPromotionListener();
    listener.setKeys(new String[] {CONTEXT_MAX_PACKAGE_NOTES_COUNT});
    return listener;
  }

  @StepScope
  @Bean("getEHoldingsPromotionListener")
  public ExecutionContextPromotionListener getEHoldingsPromotionListener() {
    var listener = new ExecutionContextPromotionListener();
    listener.setKeys(new String[] {CONTEXT_MAX_TITLE_NOTES_COUNT});
    return listener;
  }

  @Bean
  @StepScope
  public StepExecutionListener stepExecutionListener(ListItemWriter<EHoldingsResource> listItemWriter) {
    return new StepExecutionListener() {
      @Override
      public void beforeStep(@NotNull StepExecution stepExecution) {
        //not needed
      }

      @Override
      public ExitStatus afterStep(@NotNull StepExecution stepExecution) {
        var executionContext = stepExecution.getJobExecution().getExecutionContext();
        executionContext.put(CONTEXT_RESOURCES, listItemWriter.getWrittenItems());
        return stepExecution.getExitStatus();
      }
    };
  }

  @Bean
  @StepScope
  public ListItemWriter<EHoldingsResource> listItemWriter() {
    return new ListItemWriter<>();
  }

  @Bean
  @JobScope
  public EHoldingsExportConfig exportConfig(
    @Value("#{jobParameters['eHoldingsExportConfig']}") String exportConfigStr) throws JsonProcessingException {
    return objectMapper.readValue(exportConfigStr, EHoldingsExportConfig.class);
  }

  @Bean("eHoldingsItemProcessor")
  @StepScope
  public ItemProcessor<EHoldingsResource, EHoldingsResource> itemProcessor(
    EHoldingsAgreementItemProcessor eHoldingsAgreementItemProcessor,
    EHoldingsNoteItemProcessor eHoldingsNoteItemProcessor) {
    var itemProcessor = new CompositeItemProcessor<EHoldingsResource, EHoldingsResource>();
    itemProcessor.setDelegates(List.of(eHoldingsNoteItemProcessor, eHoldingsAgreementItemProcessor));
    return itemProcessor;
  }
}
