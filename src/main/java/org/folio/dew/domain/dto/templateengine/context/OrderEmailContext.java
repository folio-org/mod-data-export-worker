package org.folio.dew.domain.dto.templateengine.context;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OrderEmailContext {
  private String createdAt;
  private OrganizationContext organization;
  private List<OrderWrapper> orders;
}
