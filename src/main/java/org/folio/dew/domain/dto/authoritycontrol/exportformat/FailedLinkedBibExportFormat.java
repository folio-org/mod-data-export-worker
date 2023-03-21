package org.folio.dew.domain.dto.authoritycontrol.exportformat;

import lombok.Data;
import org.folio.dew.domain.dto.annotation.ExportFormat;
import org.folio.dew.domain.dto.annotation.ExportHeader;

@Data
@ExportFormat(sentenceCaseHeaders = true)
public class FailedLinkedBibExportFormat implements AuthorityControlExportFormat {
  private String failed;
  private String bibliographicTitle;
  @ExportHeader("Bibliographic UUID")
  private String bibliographicUUID;
  private String failedBibFieldUpdate;
  private String linkedAuthorityIdentifier;
  private String reasonForError;
}
