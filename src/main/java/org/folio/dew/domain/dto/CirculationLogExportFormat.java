package org.folio.dew.domain.dto;

import lombok.Data;

@Data
public class CirculationLogExportFormat {
  private String userBarcode;
  private String items;
  private String objectField;
  private String action;
  private String date;
  private String servicePointId;
  private String source;
  private String description;
}

