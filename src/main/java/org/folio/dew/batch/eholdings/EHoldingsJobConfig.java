package org.folio.dew.batch.eholdings;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.batch.JobCompletionNotificationListener;
import org.folio.dew.client.AgreementClient;
import org.folio.dew.client.KbEbscoClient;
import org.folio.dew.client.NotesClient;
import org.folio.dew.domain.dto.EHoldingsExportConfig;
import org.folio.dew.domain.dto.EHoldingsResourceExportFormat;
import org.folio.dew.domain.dto.ExportType;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
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

@Log4j2
@Configuration
@RequiredArgsConstructor
public class EHoldingsJobConfig {

  private static final int PROCESSING_RECORD_CHUNK_SIZE = 5;

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;
  private final KbEbscoClient kbEbscoClient;
  private final NotesClient notesClient;
  private final AgreementClient agreementClient;
  private final ObjectMapper objectMapper;

  @Bean
  public Job getEHoldingsJob(
    JobRepository jobRepository,
    JobCompletionNotificationListener jobCompletionNotificationListener,
    @Qualifier("getEHoldingsStep") Step getEHoldingsStep,
    @Qualifier("saveEholdingsStep") Step saveEholdingsStep) {
    return jobBuilderFactory
      .get(ExportType.E_HOLDINGS.toString())
      .repository(jobRepository)
      .incrementer(new RunIdIncrementer())
      .listener(jobCompletionNotificationListener)
      .flow(getEHoldingsStep)
      .next(saveEholdingsStep)
      .end()
      .build();
  }

  @Bean("getEHoldingsStep")
  public Step getEHoldingsStep(@Qualifier("eHoldingsItemProcessor")
                               ItemProcessor<EHoldingsResourceExportFormat, EHoldingsResourceExportFormat> processor,
                               EHoldingsItemReader eHoldingsCsvItemReader,
                               ListItemWriter<EHoldingsResourceExportFormat> listItemWriter,
                               NoteEholdingsItemProcessor noteEholdingsItemProcessor) {
    return stepBuilderFactory
      .get("getEHoldingsStep")
      .<EHoldingsResourceExportFormat, EHoldingsResourceExportFormat>chunk(PROCESSING_RECORD_CHUNK_SIZE)
      .reader(eHoldingsCsvItemReader)
      .processor(processor)
      .writer(listItemWriter)
      .listener(noteEholdingsItemProcessor)
      .listener(stepExecutionListener(listItemWriter))
      .listener(eholdingsPromotionListener())
      .build();
  }

  @Bean("saveEholdingsStep")
  public Step saveEholdingsStep(EholdingsListItemReader eholdingsListItemReader,
                                @Qualifier("eHoldingsWriter") EholdingsCsvWriter flatFileItemWriter,
                                EHoldingsStepListener eHoldingsStepListener) {
    return stepBuilderFactory
      .get("saveEholdingsStep")
      .<EHoldingsResourceExportFormat, EHoldingsResourceExportFormat>chunk(PROCESSING_RECORD_CHUNK_SIZE)
      .reader(eholdingsListItemReader)
      .writer(flatFileItemWriter)
      .listener(eHoldingsStepListener)
      .build();
  }

  @Bean
  public ExecutionContextPromotionListener eholdingsPromotionListener() {
    var listener = new ExecutionContextPromotionListener();
    listener.setKeys(new String[] {"holdings", "packageMaxNotesCount", "titleMaxNotesCount", "tempOutputFilePath"});
    return listener;
  }

  @Bean
  @StepScope
  public StepExecutionListener stepExecutionListener(ListItemWriter<EHoldingsResourceExportFormat> listItemWriter) {
    return new StepExecutionListener() {
      @Override
      public void beforeStep(StepExecution stepExecution) {

      }

      @Override
      public ExitStatus afterStep(StepExecution stepExecution) {
        var executionContext = stepExecution.getJobExecution().getExecutionContext();
        executionContext.put("holdings", listItemWriter.getWrittenItems());
        return stepExecution.getExitStatus();
      }
    };
  }

  @Bean
  @StepScope
  public ListItemWriter<EHoldingsResourceExportFormat> listItemWriter() {
    return new ListItemWriter<>();
  }

  @Bean("eHoldingsReader")
  @StepScope
  public EHoldingsItemReader reader(
    @Value("#{jobParameters['eHoldingsExportConfig']}") String exportConfigStr) throws JsonProcessingException {
    var eHoldingsExportConfig = objectMapper.readValue(exportConfigStr, EHoldingsExportConfig.class);
    return new EHoldingsItemReader(kbEbscoClient, eHoldingsExportConfig);
  }

  @Bean
  @StepScope
  public AgreementEholdingsItemProcessor agreementProcessor(EHoldingsToExportFormatMapper mapper,
                                                            @Value("#{jobParameters['eHoldingsExportConfig']}")
                                                            String exportConfigStr) throws JsonProcessingException {
    var config = objectMapper.readValue(exportConfigStr, EHoldingsExportConfig.class);
    var loadPackageAgreements =
      config.getPackageFields() != null && config.getPackageFields().contains("packageAgreements");
    var loadResourceAgreements = config.getTitleFields() != null && config.getTitleFields().contains("titleAgreements");

    return new AgreementEholdingsItemProcessor(agreementClient, mapper, loadPackageAgreements, loadResourceAgreements);
  }

  @Bean
  @StepScope
  public NoteEholdingsItemProcessor noteProcessor(EHoldingsToExportFormatMapper mapper,
                                                  @Value("#{jobParameters['eHoldingsExportConfig']}")
                                                  String exportConfigStr) throws JsonProcessingException {
    var config = objectMapper.readValue(exportConfigStr, EHoldingsExportConfig.class);
    var loadPackageNotes = config.getPackageFields() != null && config.getPackageFields().contains("packageNotes");
    var loadResourceNotes = config.getTitleFields() != null && config.getTitleFields().contains("titleNotes");
    return new NoteEholdingsItemProcessor(notesClient, mapper, loadPackageNotes, loadResourceNotes);
  }

  @Bean("eHoldingsItemProcessor")
  @StepScope
  public ItemProcessor<EHoldingsResourceExportFormat, EHoldingsResourceExportFormat> itemProcessor(
    AgreementEholdingsItemProcessor agreementEholdingsItemProcessor,
    NoteEholdingsItemProcessor noteEholdingsItemProcessor) {
    var itemProcessor = new CompositeItemProcessor<EHoldingsResourceExportFormat, EHoldingsResourceExportFormat>();
    itemProcessor.setDelegates(List.of(noteEholdingsItemProcessor, agreementEholdingsItemProcessor));
    return itemProcessor;
  }

  @Bean("eHoldingsWriter")
  @StepScope
  public EholdingsCsvWriter writer(
    @Value("#{jobParameters['tempOutputFilePath']}") String tempOutputFilePath,
    @Value("#{jobParameters['eHoldingsExportConfig']}") String exportConfigStr) throws JsonProcessingException {
    var eHoldingsExportConfig = objectMapper.readValue(exportConfigStr, EHoldingsExportConfig.class);
    var exportFields = new ArrayList<String>();
    if (eHoldingsExportConfig.getPackageFields() != null) {
      exportFields.addAll(eHoldingsExportConfig.getPackageFields());
    }
    if (eHoldingsExportConfig.getTitleFields() != null) {
      exportFields.addAll(eHoldingsExportConfig.getTitleFields());
    }
    if (exportFields.isEmpty()) {
      throw new IllegalArgumentException("Export fields are empty");
    }

    var names = exportFields.toArray(String[]::new);

    return new EholdingsCsvWriter(tempOutputFilePath, names);
  }
}
