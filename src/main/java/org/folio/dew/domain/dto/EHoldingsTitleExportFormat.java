package org.folio.dew.domain.dto;

import java.util.List;

import lombok.Data;

@Data
public class EHoldingsTitleExportFormat {
  private String packageId;
  private String packageName;
  private String titleId;
  private String titleName;
  private List<Object> alternateTitles;
  private String titleHoldingsStatus;
  private String publicationType;
  private String titleType;
  private List<Object> contributors;
  private String edition;
  private String publisher;
  private List<String> ISSN_Print;
  private List<String> ISSN_Online;
  private List<String> ISBN_Print;
  private List<String> ISBN_Online;
  private List<String> Subjects;
  private String peerReviewed;
  private String description;
  private String title;
  private List<Object> managedCoverage;
  private List<Object> customCoverage;
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
  private List<String> titleTags;
  private String titleAgreementStartDate;
  private String titleAgreementName;
  private String titleAgreementStatus;
  private String titleNoteLastUpdatedDate;
  private String titleNoteType;
  private String titleNoteTitle;
  private String titleNoteDetails;
}
