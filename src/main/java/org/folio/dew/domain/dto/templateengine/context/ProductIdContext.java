package org.folio.dew.domain.dto.templateengine.context;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductIdContext {
  private String productId;
  private String qualifier;
  private TypeContext productIdType;
}
