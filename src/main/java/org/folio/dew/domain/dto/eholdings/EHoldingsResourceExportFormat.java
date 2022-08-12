package org.folio.dew.domain.dto.eholdings;

import java.util.Collections;
import java.util.List;

import lombok.Data;

@Data
public class EHoldingsResourceExportFormat {
  //Package fields
  private String providerName;
  private String providerId;
  private String packageName;
  private String packageId;
  private String packageType;
  private String packageHoldingsStatus;
  private String packageContentType;
  private String packageCustomCoverage;
  private String packageShowToPatrons;
  private String packageAutomaticallySelect;
  private String packageProxy;
  private String packageAccessStatusType;
  private String packageTags;
  private String packageAgreements;
  private List<String> packageNotes = Collections.emptyList();

  //Title fields
  private String titleId;
  private String titleName;
  private String alternateTitles;
  private String titleHoldingsStatus;
  private String publicationType;
  private String titleType;
  private String contributors;
  private String edition;
  private String publisher;
  private String ISSNPrint;
  private String ISSNOnline;
  private String ISBNPrint;
  private String ISBNOnline;
  private String subjects;
  private String peerReviewed;
  private String description;
  private String title;
  private String managedCoverage;
  private String customCoverage;
  private String coverageStatement;
  private String managedEmbargo;
  private String customEmbargo;
  private String titleShowToPatrons;
  private String titleProxy;
  private String url;
  private String titleAccessStatusType;
  private String customValue1;
  private String customValue2;
  private String customValue3;
  private String customValue4;
  private String customValue5;
  private String titleTags;
  private String titleAgreements;
  private List<String> titleNotes = Collections.emptyList();
}
