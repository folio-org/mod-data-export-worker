package org.folio.dew.domain.dto.authoritycontrol;

import lombok.Data;
import org.folio.dew.domain.dto.annotation.ExportFormat;
import org.folio.dew.domain.dto.annotation.ExportHeader;

@Data
@ExportFormat(sentenceCaseHeaders = true)
public class AuthorityUpdateHeadingExportFormat {
  private String lastUpdated;
  private String originalHeading;
  private String newHeading;
  private String identifier;
  @ExportHeader("Original 1XX")
  private String originalHeadingType;
  @ExportHeader("New 1XX")
  private String newHeadingType;
  private String authoritySourceFileName;
  private String numberOfBibliographicRecordsLinked;
  private String updater;
}
