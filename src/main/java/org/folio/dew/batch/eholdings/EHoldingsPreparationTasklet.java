package org.folio.dew.batch.eholdings;

import static org.folio.dew.batch.eholdings.EHoldingsJobConstants.CONTEXT_MAX_PACKAGE_NOTES_COUNT;
import static org.folio.dew.batch.eholdings.EHoldingsJobConstants.CONTEXT_TOTAL_PACKAGES;
import static org.folio.dew.batch.eholdings.EHoldingsJobConstants.LOAD_FIELD_PACKAGE_AGREEMENTS;
import static org.folio.dew.batch.eholdings.EHoldingsJobConstants.LOAD_FIELD_PACKAGE_NOTES;
import static org.folio.dew.batch.eholdings.EHoldingsJobConstants.LOAD_FIELD_PROVIDER_TOKEN;
import static org.folio.dew.client.AgreementClient.getFiltersParam;
import static org.folio.dew.client.KbEbscoClient.ACCESS_TYPE;
import static org.folio.dew.client.NotesClient.NoteLinkDomain.EHOLDINGS;
import static org.folio.dew.domain.dto.EHoldingsExportConfig.RecordTypeEnum.PACKAGE;

import org.folio.dew.client.AgreementClient;
import org.folio.dew.client.KbEbscoClient;
import org.folio.dew.client.NotesClient;
import org.folio.dew.domain.dto.EHoldingsExportConfig;
import org.folio.dew.domain.dto.eholdings.EHoldingsPackageDTO;
import org.folio.dew.repository.EHoldingsPackageRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.log4j.Log4j2;

@Component
@JobScope
@Log4j2
public class EHoldingsPreparationTasklet implements Tasklet {

  @Value("#{jobExecution}")
  private JobExecution jobExecution;

  private final KbEbscoClient kbEbscoClient;
  private final NotesClient notesClient;
  private final AgreementClient agreementClient;

  private final EHoldingsExportConfig.RecordTypeEnum recordType;
  private final String recordId;
  private final boolean loadNotes;
  private final boolean loadAgreements;
  private final boolean loadProvider;

  private final EHoldingsPackageDTO eHoldingsPackageDTO;

  private final EHoldingsPackageRepository repository;


  public EHoldingsPreparationTasklet(KbEbscoClient kbEbscoClient,
                                     NotesClient notesClient,
                                     AgreementClient agreementClient,
                                     EHoldingsExportConfig exportConfig, EHoldingsPackageRepository repository) {
    this.repository = repository;
    this.notesClient = notesClient;
    this.kbEbscoClient = kbEbscoClient;
    this.agreementClient = agreementClient;
    this.recordId = exportConfig.getRecordId();
    this.recordType = exportConfig.getRecordType();
    this.eHoldingsPackageDTO = new EHoldingsPackageDTO();

    var packageFields = exportConfig.getPackageFields();
    this.loadNotes = packageFields != null && packageFields.contains(LOAD_FIELD_PACKAGE_NOTES);
    this.loadProvider = packageFields != null && packageFields.contains(LOAD_FIELD_PROVIDER_TOKEN);
    this.loadAgreements = packageFields != null && packageFields.contains(LOAD_FIELD_PACKAGE_AGREEMENTS);
  }

  @Override
  public RepeatStatus execute(@NotNull StepContribution stepContribution, @NotNull ChunkContext chunkContext) {
    var packageId = recordType == PACKAGE ? recordId : recordId.split("-\\d+$")[0];
    log.trace("Reading record with id: " + packageId);

    eHoldingsPackageDTO.setEPackage(kbEbscoClient.getPackageById(packageId, ACCESS_TYPE));
    if (loadNotes) loadNotes();
    if (loadProvider) loadProvider();
    if (loadAgreements) loadAgreements();
    log.trace("Record is read.");

    log.trace("Writing the record to a database.");
    var ePackage = EHoldingsPackageMapper.convertToEntity(eHoldingsPackageDTO);
    var jobId = jobExecution.getJobId();
    ePackage.setJobExecutionId(jobId);
    repository.save(ePackage);
    jobExecution.getExecutionContext().putInt(CONTEXT_TOTAL_PACKAGES,
      jobExecution.getExecutionContext().getInt(CONTEXT_TOTAL_PACKAGES, 0) + 1);

    var noteCollectionSize = eHoldingsPackageDTO.getNotes() != null ?
      eHoldingsPackageDTO.getNotes().size() : 0;

    if (noteCollectionSize > 0) {
      var packageMaxNotesCount =
        jobExecution.getExecutionContext().getInt(CONTEXT_MAX_PACKAGE_NOTES_COUNT, 0);
      if (packageMaxNotesCount < noteCollectionSize) {
        jobExecution.getExecutionContext()
          .putInt(CONTEXT_MAX_PACKAGE_NOTES_COUNT, noteCollectionSize);
      }
    }
    log.trace("Records are written to a database.");
    return RepeatStatus.FINISHED;
  }

  private void loadProvider() {
    log.trace("Loading provider...");
    var providerId = eHoldingsPackageDTO.getEPackage().getData().getAttributes().getProviderId();
    eHoldingsPackageDTO.setEProvider(kbEbscoClient.getProviderById(providerId, null));
    log.trace("Provider loaded.");
  }

  private void loadNotes() {
    log.trace("Loading notes...");
    var noteCollection = notesClient.getAssignedNotes(EHOLDINGS,
      NotesClient.NoteLinkType.PACKAGE,
      eHoldingsPackageDTO.getEPackage().getData().getId());
    if (noteCollection.getTotalRecords() > 0) {
      eHoldingsPackageDTO.setNotes(noteCollection.getNotes());
    }
    log.trace("Notes loaded.");
  }

  private void loadAgreements() {
    log.trace("Loading agreements...");
    var agreements = agreementClient.getAssignedAgreements(
      getFiltersParam(eHoldingsPackageDTO.getEPackage().getData().getId()));
    if (!agreements.isEmpty()) {
      eHoldingsPackageDTO.setAgreements(agreements);
    }
    log.trace("Agreements loaded.");
  }
}
