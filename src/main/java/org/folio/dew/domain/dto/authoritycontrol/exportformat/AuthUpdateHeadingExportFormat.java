package org.folio.dew.domain.dto.authoritycontrol.exportformat;

import static org.folio.dew.domain.dto.authoritycontrol.exportformat.ExportFormatHeaders.AUTHORITY_RECORD_TYPE;
import static org.folio.dew.domain.dto.authoritycontrol.exportformat.ExportFormatHeaders.NEW_1XX;
import static org.folio.dew.domain.dto.authoritycontrol.exportformat.ExportFormatHeaders.ORIGINAL_1XX;

import lombok.Data;
import org.folio.dew.domain.dto.annotation.ExportFormat;
import org.folio.dew.domain.dto.annotation.ExportHeader;

@Data
@ExportFormat(sentenceCaseHeaders = true)
public class AuthUpdateHeadingExportFormat implements AuthorityControlExportFormat {
  private String lastUpdated;
  private String originalHeading;
  private String newHeading;
  private String identifier;
  @ExportHeader(ORIGINAL_1XX)
  private String originalHeadingType;
  @ExportHeader(NEW_1XX)
  private String newHeadingType;
  private String authoritySourceFileName;
  private String numberOfBibliographicRecordsLinked;
  @ExportHeader(AUTHORITY_RECORD_TYPE)
  private String source;
  private String updater;
}
