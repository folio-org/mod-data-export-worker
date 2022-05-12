package org.folio.dew.domain.dto;

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
  private String packageContentType;
  private String packageCustomCoverage;
  private String packageShowToPatrons;
  private String packageAutomaticallySelect;
  private String packageProxy;
  private String packageAccessStatusType;
  private String packageTags;
  private String packageAgreementStartDate;
  private String packageAgreementName;
  private String packageAgreementStatus;
  private String packageNoteLastUpdatedDate;
  private String packageNoteType;
  private String packageNoteTitle;
  private String packageNoteDetails;

  //Title fields
  private String titleId;
  private String titleName;
  private String alternateTitles;
  private String titleHoldingsStatus;
  private String publicationType;
  private String titleType;
  private List<String> contributors;
  private String edition;
  private String publisher;
  private String ISSN_Print;
  private String ISSN_Online;
  private String ISBN_Print;
  private String ISBN_Online;
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
}
