package org.folio.dew.domain.dto.eholdings;

import java.util.Collections;
import java.util.List;
import lombok.Data;

@Data
public class EHoldingsResourceExportFormat {
  //Title fields, the declaration order defines the column order in the exported file
  private String titleName;
  private String alternateTitles;
  private String titleId;
  private String publicationType;
  private String titleType;
  private String titleHoldingsStatus;
  private String hideInPublicationFinder;
  private String hideInFullTextFinder;
  private String excludeFromMARCExport;
  private String managedCoverage;
  private String managedEmbargo;
  private String customCoverage;
  private String customEmbargo;
  private String coverageStatement;
  private String titleProxy;
  private String titleUrl;
  private String titleProxiedUrl;
  private String titleAccessStatusType;
  private String titleTags;
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
  private String customValue1;
  private String customValue2;
  private String customValue3;
  private String customValue4;
  private String customValue5;
  private String titleAgreements;
  private List<String> titleNotes = Collections.emptyList();
}
