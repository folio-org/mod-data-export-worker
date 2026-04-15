package org.folio.dew.domain.dto.acquisitions.edifact;

import lombok.Data;

import java.util.UUID;

@Data
public class TenantAddress {
  private UUID id;
  private String address;
}
