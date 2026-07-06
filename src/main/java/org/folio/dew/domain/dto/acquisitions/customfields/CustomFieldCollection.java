package org.folio.dew.domain.dto.acquisitions.customfields;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomFieldCollection {
  private List<CustomField> customFields;
}