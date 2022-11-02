package org.folio.dew.domain.dto.eholdings;

import java.util.Collections;
import java.util.List;
import lombok.Data;

@Data
public class EHoldingsPackageExportFormat {
  //Provider fields
  private String providerId;
  private String providerName;
  private String providerLevelToken;

  //Package fields
  private String packageId;
  private String packageName;
  private String packageType;
  private String packageHoldingsStatus;
  private String packageContentType;
  private String packageCustomCoverage;
  private String packageShowToPatrons;
  private String packageAutomaticallySelect;
  private String packageLevelToken;
  private String packageProxy;
  private String packageAccessStatusType;
  private String packageTags;
  private String packageAgreements;
  private List<String> packageNotes = Collections.emptyList();
}
