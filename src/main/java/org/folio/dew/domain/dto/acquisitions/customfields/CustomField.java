package org.folio.dew.domain.dto.acquisitions.customfields;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomField {
  private String refId;
  private String name;
  private String type;
  private Boolean visible;
  private SelectField selectField;
}
