package org.folio.dew.batch.eholdings;

import static org.folio.dew.client.NotesClient.NoteLinkDomain.EHOLDINGS;
import static org.folio.dew.client.NotesClient.NoteLinkType.PACKAGE;
import static org.folio.dew.client.NotesClient.NoteLinkType.RESOURCE;

import lombok.extern.log4j.Log4j2;
import org.folio.dew.client.NotesClient;
import org.folio.dew.domain.dto.EHoldingsResourceExportFormat;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;

@Log4j2
public class NoteEholdingsItemProcessor
  implements ItemProcessor<EHoldingsResourceExportFormat, EHoldingsResourceExportFormat> {

  private final NotesClient notesClient;
  private final EHoldingsToExportFormatMapper mapper;
  private final boolean loadPackageNotes;
  private final boolean loadResourceNotes;
  private StepExecution stepExecution;

  public NoteEholdingsItemProcessor(NotesClient notesClient, EHoldingsToExportFormatMapper mapper,
                                    boolean loadPackageNotes, boolean loadResourceNotes) {
    this.notesClient = notesClient;
    this.mapper = mapper;
    this.loadPackageNotes = loadPackageNotes;
    this.loadResourceNotes = loadResourceNotes;
  }

  @BeforeStep
  public void beforeStep(StepExecution stepExecution) {
    this.stepExecution = stepExecution;
  }

  @Override
  public EHoldingsResourceExportFormat process(EHoldingsResourceExportFormat exportFormat) throws Exception {
    if (loadPackageNotes) {
      var packageId = exportFormat.getPackageId();
      var noteCollection = notesClient.getAssignedNotes(EHOLDINGS, PACKAGE, packageId);
      if (noteCollection.getTotalRecords() > 0) {
        exportFormat.setPackageNotes(mapper.convertNotes(noteCollection.getNotes()));
        var packageMaxNotesCount = stepExecution.getExecutionContext().getInt("packageMaxNotesCount", 0);
        if (packageMaxNotesCount < noteCollection.getTotalRecords()) {
          stepExecution.getExecutionContext().putInt("packageMaxNotesCount", noteCollection.getTotalRecords());
        }
      }
    }
    if (loadResourceNotes) {
      var resourceId = exportFormat.getPackageId() + "-" + exportFormat.getTitleId();
      var noteCollection = notesClient.getAssignedNotes(EHOLDINGS, RESOURCE, resourceId);
      if (noteCollection.getTotalRecords() > 0) {
        exportFormat.setTitleNotes(mapper.convertNotes(noteCollection.getNotes()));
        var packageMaxNotesCount = stepExecution.getExecutionContext().getInt("titleMaxNotesCount", 0);
        if (packageMaxNotesCount < noteCollection.getTotalRecords()) {
          stepExecution.getExecutionContext().putInt("titleMaxNotesCount", noteCollection.getTotalRecords());
        }
      }
    }
    return exportFormat;
  }

}
