package org.folio.dew.domain.dto.templateengine.context;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CostContext {
  private String listUnitPrice;
  private String listUnitPriceElectronic;
  private Integer quantityPhysical;
  private Integer quantityElectronic;
  private String poLineEstimatedPrice;
  private String currency;
}
