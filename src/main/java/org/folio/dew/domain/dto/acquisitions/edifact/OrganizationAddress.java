package org.folio.dew.domain.dto.acquisitions.edifact;

import lombok.Data;

@Data
public class OrganizationAddress {
  private String addressLine1;
  private String city;
  private String zipCode;
  private String country;
  private Boolean isPrimary;
}
