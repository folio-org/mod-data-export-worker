package org.folio.dew.domain.dto.authoritycontrol;

import lombok.Data;
import org.folio.dew.domain.dto.annotation.ExportFormat;

@Data
@ExportFormat(sentenceCaseHeaders = true)
public class FailedLinkedBibExportFormat implements AuthorityControlExportFormat {
  private String failed;
  private String bibliographicTitle;
  private String bibliographicUUID;
  private String failedBibFieldUpdate;
  private String linkedAuthorityIdentifier;
  private String reasonForError;
}
