package org.folio.dew.domain.dto;

import java.util.List;

import lombok.Data;

@Data
public class EHoldingsTitleExportFormat {
  private String resourceId;
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
  private String Subjects;
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
  private String CustomValue1;
  private String CustomValue2;
  private String CustomValue3;
  private String CustomValue4;
  private String CustomValue5;
  private String titleTags;
  private String titleAgreementStartDate;
  private String titleAgreementName;
  private String titleAgreementStatus;
  private String titleNoteLastUpdatedDate;
  private String titleNoteType;
  private String titleNoteTitle;
  private String titleNoteDetails;
}
