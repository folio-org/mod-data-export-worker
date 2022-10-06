package org.folio.dew.batch.eholdings;

import static org.folio.dew.batch.eholdings.EHoldingsJobConstants.LOAD_FIELD_TITLE_NOTES;
import static org.folio.dew.client.NotesClient.NoteLinkDomain.EHOLDINGS;
import static org.folio.dew.client.NotesClient.NoteLinkType.RESOURCE;

import org.folio.dew.client.NotesClient;
import org.folio.dew.domain.dto.EHoldingsExportConfig;
import org.folio.dew.domain.dto.eholdings.EHoldingsResourceDTO;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class EHoldingsNoteItemProcessor
  implements ItemProcessor<EHoldingsResourceDTO, EHoldingsResourceDTO> {

  private final NotesClient notesClient;
  private final boolean loadResourceNotes;

  public EHoldingsNoteItemProcessor(NotesClient notesClient,
                                    EHoldingsExportConfig exportConfig) {
    this.notesClient = notesClient;
    this.loadResourceNotes =
      exportConfig.getTitleFields() != null && exportConfig.getTitleFields().contains(LOAD_FIELD_TITLE_NOTES);
  }

  @Override
  public EHoldingsResourceDTO process(@NotNull EHoldingsResourceDTO eHoldingsResourceDTO) throws Exception {
    if (loadResourceNotes) {
      var resourceDataAttributes = eHoldingsResourceDTO.getResourcesData().getAttributes();
      var resourceId = resourceDataAttributes.getPackageId() + "-" + resourceDataAttributes.getTitleId();
      var noteCollection = notesClient.getAssignedNotes(EHOLDINGS, RESOURCE, resourceId);
      if (noteCollection.getTotalRecords() > 0) {
        eHoldingsResourceDTO.setNotes(noteCollection.getNotes());
      }
    }
    return eHoldingsResourceDTO;
  }

}
