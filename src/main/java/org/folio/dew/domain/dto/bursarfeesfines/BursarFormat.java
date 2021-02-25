package org.folio.dew.domain.dto.bursarfeesfines;

import lombok.Data;

@Data
public class BursarFormat {
  private String employeeId;
  private String amount;
  private String itemType;
  private String transactionDate;
  private String sfs;
  private String termValue;
  private String description;
}
