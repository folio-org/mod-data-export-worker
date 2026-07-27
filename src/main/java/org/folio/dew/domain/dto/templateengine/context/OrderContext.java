package org.folio.dew.domain.dto.templateengine.context;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderContext {
  private String poNumber;
  private String orderType;
  private OrderMetadataContext metadata;
  private TenantAddressContext shipTo;
  private TenantAddressContext billTo;
}
