package org.folio.dew.batch.eholdings;

import static org.folio.dew.batch.eholdings.EHoldingsJobConstants.CONTEXT_MAX_PACKAGE_NOTES_COUNT;
import static org.folio.dew.batch.eholdings.EHoldingsJobConstants.CONTEXT_MAX_TITLE_NOTES_COUNT;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.batch.JobCompletionNotificationListener;
import org.folio.dew.config.properties.EHoldingsJobProperties;
import org.folio.dew.domain.dto.EHoldingsExportConfig;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.eholdings.EHoldingsResourceDTO;
import org.folio.dew.domain.dto.eholdings.EHoldingsResourceExportFormat;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.listener.ExecutionContextPromotionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Log4j2
@Configuration
@RequiredArgsConstructor
public class EHoldingsJobConfig {

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;
  private final ObjectMapper objectMapper;
  private final EHoldingsJobProperties jobProperties;

  @Bean
  public Job getEHoldingsJob(
    JobRepository jobRepository,
    JobCompletionNotificationListener jobCompletionNotificationListener,
    @Qualifier("prepareEHoldingsStep") Step prepareEHoldingsStep,
    @Qualifier("getEHoldingsStep") Step getEHoldingsStep,
    @Qualifier("saveEHoldingsStep") Step saveEHoldingsStep,
    @Qualifier("cleanupEHoldingsStep") Step cleanupEHoldingsStep) {
    return jobBuilderFactory
      .get(ExportType.E_HOLDINGS.toString())
      .repository(jobRepository)
      .incrementer(new RunIdIncrementer())
      .listener(jobCompletionNotificationListener)
      .start(prepareEHoldingsStep)
      .next(getEHoldingsStep)
      .next(saveEHoldingsStep)
      .next(cleanupEHoldingsStep)
      .build();
  }

  @Bean("prepareEHoldingsStep")
  public Step prepareEHoldingsStep(EHoldingsPreparationTasklet tasklet,
                                   @Qualifier("prepareEHoldingsPromotionListener")
                                   ExecutionContextPromotionListener prepareEHoldingsPromotionListener) {
    return stepBuilderFactory
      .get("prepareEHoldingsStep")
      .tasklet(tasklet)
      .listener(prepareEHoldingsPromotionListener)
      .build();
  }

  @Bean("getEHoldingsStep")
  public Step getEHoldingsStep(@Qualifier("eHoldingsItemProcessor")
                               ItemProcessor<EHoldingsResourceDTO, EHoldingsResourceDTO> processor,
                               @Qualifier("getEHoldingsPromotionListener")
                               ExecutionContextPromotionListener getEHoldingsPromotionListener,
                               EHoldingsItemReader eHoldingsItemReader,
                               GetEHoldingsWriter getEHoldingsWriter,
                               EHoldingsNoteItemProcessor eHoldingsNoteItemProcessor) {
    return stepBuilderFactory
      .get("getEHoldingsStep")
      .<EHoldingsResourceDTO, EHoldingsResourceDTO>chunk(jobProperties.getJobChunkSize())
      .reader(eHoldingsItemReader)
      .processor(processor)
      .writer(getEHoldingsWriter)
      .listener(eHoldingsNoteItemProcessor)
      .listener(getEHoldingsPromotionListener)
      .build();
  }

  @Bean("saveEHoldingsStep")
  public Step saveEHoldingsStep(DatabaseEHoldingsReader databaseEHoldingsReader,
                                EHoldingsCsvFileWriter flatFileItemWriter,
                                EHoldingsStepListener eHoldingsStepListener,
                                ItemProcessor<EHoldingsResourceDTO, EHoldingsResourceExportFormat> resourceProcessor) {
    return stepBuilderFactory
      .get("saveEHoldingsStep")
      .<EHoldingsResourceDTO, EHoldingsResourceExportFormat>chunk(jobProperties.getJobChunkSize())
      .reader(databaseEHoldingsReader)
      .processor(resourceProcessor)
      .writer(flatFileItemWriter)
      .listener(eHoldingsStepListener)
      .build();
  }

  @Bean("cleanupEHoldingsStep")
  public Step cleanupEHoldingsStep(EHoldingsCleanupTasklet cleanupTasklet) {
    return stepBuilderFactory
      .get("cleanupEHoldingsStep")
      .tasklet(cleanupTasklet)
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

  @JobScope
  @Bean("eHoldingsExportConfig")
  public EHoldingsExportConfig exportConfig(
    @Value("#{jobParameters['eHoldingsExportConfig']}") String exportConfigStr) throws JsonProcessingException {
    return objectMapper.readValue(exportConfigStr, EHoldingsExportConfig.class);
  }

  @StepScope
  @Bean("eHoldingsItemProcessor")
  public ItemProcessor<EHoldingsResourceDTO, EHoldingsResourceDTO> itemProcessor(
    EHoldingsAgreementItemProcessor eHoldingsAgreementItemProcessor,
    EHoldingsNoteItemProcessor eHoldingsNoteItemProcessor) {
    var itemProcessor = new CompositeItemProcessor<EHoldingsResourceDTO, EHoldingsResourceDTO>();
    itemProcessor.setDelegates(List.of(eHoldingsNoteItemProcessor, eHoldingsAgreementItemProcessor));
    return itemProcessor;
  }

  @Bean("eHoldingsResourceProcessor")
  public ItemProcessor<EHoldingsResourceDTO, EHoldingsResourceExportFormat> resourceProcessor(
    EHoldingsToExportFormatMapper mapper) {
    return mapper::convertToExportFormat;
  }
}
