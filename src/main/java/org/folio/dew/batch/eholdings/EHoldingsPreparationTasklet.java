package org.folio.dew.batch.eholdings;

import static org.folio.dew.batch.eholdings.EHoldingsJobConstants.CONTEXT_MAX_PACKAGE_NOTES_COUNT;
import static org.folio.dew.batch.eholdings.EHoldingsJobConstants.CONTEXT_PACKAGE;
import static org.folio.dew.batch.eholdings.EHoldingsJobConstants.LOAD_FIELD_PACKAGE_AGREEMENTS;
import static org.folio.dew.batch.eholdings.EHoldingsJobConstants.LOAD_FIELD_PACKAGE_NOTES;
import static org.folio.dew.client.AgreementClient.getFiltersParam;
import static org.folio.dew.client.KbEbscoClient.ACCESS_TYPE;
import static org.folio.dew.client.NotesClient.NoteLinkDomain.EHOLDINGS;
import static org.folio.dew.domain.dto.EHoldingsExportConfig.RecordTypeEnum.PACKAGE;

import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import org.folio.dew.client.AgreementClient;
import org.folio.dew.client.KbEbscoClient;
import org.folio.dew.client.NotesClient;
import org.folio.dew.domain.dto.EHoldingsExportConfig;
import org.folio.dew.domain.dto.EHoldingsExportConfig.RecordTypeEnum;
import org.folio.dew.domain.dto.eholdings.EHoldingsPackage;

@Component
@StepScope
public class EHoldingsPreparationTasklet implements Tasklet, StepExecutionListener {

  private final KbEbscoClient kbEbscoClient;
  private final NotesClient notesClient;
  private final AgreementClient agreementClient;

  private final RecordTypeEnum recordType;
  private final String recordId;
  private final boolean loadNotes;
  private final boolean loadAgreements;

  private final EHoldingsPackage eHoldingsPackage;

  public EHoldingsPreparationTasklet(KbEbscoClient kbEbscoClient,
                                     NotesClient notesClient,
                                     AgreementClient agreementClient,
                                     EHoldingsExportConfig exportConfig) {
    this.kbEbscoClient = kbEbscoClient;
    this.notesClient = notesClient;
    this.agreementClient = agreementClient;
    this.recordId = exportConfig.getRecordId();
    this.recordType = exportConfig.getRecordType();
    this.loadNotes = exportConfig.getPackageFields() != null && exportConfig.getPackageFields().contains(LOAD_FIELD_PACKAGE_NOTES);
    this.loadAgreements = exportConfig.getPackageFields() != null && exportConfig.getPackageFields().contains(LOAD_FIELD_PACKAGE_AGREEMENTS);

    this.eHoldingsPackage = new EHoldingsPackage();
  }

  @Override
  public RepeatStatus execute(@NotNull StepContribution stepContribution, @NotNull ChunkContext chunkContext) throws Exception {
    var stepExecutionContext = stepContribution.getStepExecution().getExecutionContext();
    var packageId = recordType == PACKAGE ? recordId : recordId.split("-\\d+$")[0];

    eHoldingsPackage.setEPackage(kbEbscoClient.getPackageById(packageId, ACCESS_TYPE));
    if (loadNotes) loadNotes(stepExecutionContext);
    if (loadAgreements) loadAgreements();

    return RepeatStatus.FINISHED;
  }

  private void loadNotes(ExecutionContext stepExecutionContext) {
    var noteCollection = notesClient.getAssignedNotes(EHOLDINGS,
      NotesClient.NoteLinkType.PACKAGE,
      eHoldingsPackage.getEPackage().getData().getId());
    if (noteCollection.getTotalRecords() > 0) {
      eHoldingsPackage.setNotes(noteCollection.getNotes());
      var packageMaxNotesCount = stepExecutionContext.getInt(CONTEXT_MAX_PACKAGE_NOTES_COUNT, 0);
      if (packageMaxNotesCount < noteCollection.getTotalRecords()) {
        stepExecutionContext.putInt(CONTEXT_MAX_PACKAGE_NOTES_COUNT, noteCollection.getTotalRecords());
      }
    }
  }

  private void loadAgreements() {
    var agreements = agreementClient.getAssignedAgreements(
      getFiltersParam(eHoldingsPackage.getEPackage().getData().getId()));
    if (!agreements.isEmpty()) {
      eHoldingsPackage.setAgreements(agreements);
    }
  }

  @Override
  public void beforeStep(@NotNull StepExecution stepExecution) {
    //not needed
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    stepExecution
      .getJobExecution()
      .getExecutionContext()
      .put(CONTEXT_PACKAGE, this.eHoldingsPackage);
    return ExitStatus.COMPLETED;
  }
}
