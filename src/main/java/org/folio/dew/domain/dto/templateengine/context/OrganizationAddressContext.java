package org.folio.dew.domain.dto.templateengine.context;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrganizationAddressContext {
  private String addressLine1;
  private String city;
  private String zipCode;
  private String country;
}
