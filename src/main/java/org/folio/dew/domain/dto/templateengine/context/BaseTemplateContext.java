package org.folio.dew.domain.dto.templateengine.context;

import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
public abstract class BaseTemplateContext {
  private String createdAt;
  private OrganizationContext organization;
  private List<OrderWrapper> orders;
}
