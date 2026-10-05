package org.folio.dew.domain.dto.acquisitions.customfields;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SelectField {
  private SelectFieldOptions options;
}
