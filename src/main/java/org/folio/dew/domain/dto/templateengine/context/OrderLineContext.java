package org.folio.dew.domain.dto.templateengine.context;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OrderLineContext {
  private String poLineNumber;
  private String titleOrPackage;
  private String publisher;
  private String publicationDate;
  private String edition;
  private Boolean rush;
  private List<ContributorContext> contributors;
  private DetailsContext details;
  private CostContext cost;
  private List<FundDistributionContext> fundDistribution;
  private VendorDetailContext vendorDetail;
}
