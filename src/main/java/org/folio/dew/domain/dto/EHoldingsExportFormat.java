package org.folio.dew.domain.dto;

import java.util.List;

import lombok.Data;

@Data
public class EHoldingsExportFormat {
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
  private List<EHoldingsTitleExportFormat> titles;
}
