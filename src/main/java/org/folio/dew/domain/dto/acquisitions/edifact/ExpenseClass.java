package org.folio.dew.domain.dto.acquisitions.edifact;

import lombok.Data;

import java.util.UUID;

@Data
public class ExpenseClass {
  private UUID id;
  private String code;
}
