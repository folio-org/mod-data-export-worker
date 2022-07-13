package org.folio.dew.batch.eholdings;

import static org.folio.dew.batch.eholdings.EHoldingsJobConstants.CONTEXT_MAX_TITLE_NOTES_COUNT;
import static org.folio.dew.batch.eholdings.EHoldingsJobConstants.LOAD_FIELD_TITLE_NOTES;
import static org.folio.dew.client.NotesClient.NoteLinkDomain.EHOLDINGS;
import static org.folio.dew.client.NotesClient.NoteLinkType.RESOURCE;

import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import org.folio.dew.client.NotesClient;
import org.folio.dew.domain.dto.EHoldingsExportConfig;
import org.folio.dew.domain.dto.eholdings.EHoldingsResource;

@Component
@StepScope
public class EHoldingsNoteItemProcessor
  implements ItemProcessor<EHoldingsResource, EHoldingsResource> {

  private final NotesClient notesClient;
  private final boolean loadResourceNotes;
  private StepExecution stepExecution;

  public EHoldingsNoteItemProcessor(NotesClient notesClient,
                                    EHoldingsExportConfig exportConfig) {
    this.notesClient = notesClient;
    this.loadResourceNotes = exportConfig.getTitleFields() != null && exportConfig.getTitleFields().contains(LOAD_FIELD_TITLE_NOTES);
  }

  @BeforeStep
  public void beforeStep(@NotNull StepExecution stepExecution) {
    this.stepExecution = stepExecution;
  }

  @Override
  public EHoldingsResource process(@NotNull EHoldingsResource eHoldingsResource) throws Exception {
    if (loadResourceNotes) {
      var resourceDataAttributes = eHoldingsResource.getResourcesData().getAttributes();
      var resourceId = resourceDataAttributes.getPackageId() + "-" + resourceDataAttributes.getTitleId();
      var noteCollection = notesClient.getAssignedNotes(EHOLDINGS, RESOURCE, resourceId);
      if (noteCollection.getTotalRecords() > 0) {
        eHoldingsResource.setNotes(noteCollection.getNotes());
        var packageMaxNotesCount = stepExecution.getExecutionContext().getInt(CONTEXT_MAX_TITLE_NOTES_COUNT, 0);
        if (packageMaxNotesCount < noteCollection.getTotalRecords()) {
          stepExecution.getExecutionContext().putInt(CONTEXT_MAX_TITLE_NOTES_COUNT, noteCollection.getTotalRecords());
        }
      }
    }
    return eHoldingsResource;
  }

}
