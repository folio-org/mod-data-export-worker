package org.folio.dew.domain.dto.acquisitions.edifact;

import java.util.List;

import lombok.Data;

@Data
public class Organization {
  private String code;
  private String name;
  private List<OrganizationAddress> addresses;
}
