package org.folio.dew.domain.dto.authoritycontrol;

import lombok.Data;

@Data
public class AuthorityControlExportFormat {
  private String lastUpdated;
  private String originalHeading;
  private String newHeading;
  private String originalIdentifier;
  private String newIdentifier;
  private String original1XX;
  private String new1XXX;
  private String originalAuthoritySourceFileName;
  private String newAuthoritySourceFileName;
  private String totalNumberOfBibliographicRecords;
  private String numberOfBibliographicRecordsLinked;
  private String numberOfBibliographicRecordsNotLinked;
  private String updater;
}
