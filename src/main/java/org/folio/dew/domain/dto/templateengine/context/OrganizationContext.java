package org.folio.dew.domain.dto.templateengine.context;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrganizationContext {
  private String name;
  private OrganizationAddressContext primaryAddress;
}
