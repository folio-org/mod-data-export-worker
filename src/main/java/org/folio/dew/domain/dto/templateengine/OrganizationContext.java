package org.folio.dew.domain.dto.templateengine;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrganizationContext {

  private String name;
  private String code;
  private String contactEmail;
  private OrganizationAddressContext address;
}
