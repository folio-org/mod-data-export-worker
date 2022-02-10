package org.folio.dew.domain.dto.acquisitions.edifact;

import lombok.Data;

@Data
public class EdiFileConfig {
  private String fileId;
  private String libEdiCode;
  private String libEdiType;
  private String vendorEdiCode;
  private String vendorEdiType;
}
