package org.folio.dew.domain.dto.authoritycontrol;

import lombok.Data;

@Data
public class AuthorityControlExportFormat {
  private String lastUpdated;
  private String originalHeading;
  private String newHeading;
  private String identifier;
  private String original1XX;
  private String new1XXX;
  private String authoritySourceFileName;
  private String numberOfBibliographicRecordsLinked;
  private String updater;
}
