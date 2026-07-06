package org.folio.dew.domain.dto.acquisitions.customfields;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SelectFieldOption {
  private String id;
  private String value;
}