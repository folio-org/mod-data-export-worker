package org.folio.dew.domain.dto.eholdings;

import java.util.Collections;
import java.util.List;
import lombok.Data;

@Data
public class EHoldingsPackageExportFormat {
  //Provider fields
  private String providerLevelToken;
  private String packageLevelToken;
  private String providerName;
  private String providerId;

  //Package fields
  private String packageName;
  private String packageDisplayName;
  private String managedAlternativeNames;
  private String customAlternativeNames;
  private String packageId;
  private String packageType;
  private String packageContentType;
  private String packageAccess;
  private String managedDescription;
  private String customDescription;
  private String packageHoldingsStatus;
  private String packageCustomCoverage;
  private String hideInPublicationFinder;
  private String hideInFullTextFinder;
  private String excludeFromMARCExport;
  private String packageAutomaticallySelect;
  private String packageProxy;
  private String packageUrl;
  private String packageAccessStatusType;
  private String packageTags;
  private String packageAgreements;
  private List<String> packageNotes = Collections.emptyList();
}
