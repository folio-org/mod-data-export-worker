package org.folio.dew.domain.dto.authoritycontrol;

import lombok.Data;

@Data
public class AuthorityUpdateHeadingExportFormat {
  private String lastUpdated;
  private String originalHeading;
  private String newHeading;
  private String identifier;
  private String originalHundredthField;
  private String newHundredthField;
  private String authoritySourceFileName;
  private String numberOfBibliographicRecordsLinked;
  private String updater;
}
