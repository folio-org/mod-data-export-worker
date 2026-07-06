package org.folio.dew.domain.dto.templateengine.context;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

/**
 * A resolved select-field option: the stored option-id together with its human-readable label.
 * Built for select custom fields only.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomFieldOptionValue {
  private String id;    // stored option-id (e.g. opt_1)
  private String label; // resolved option label, falling back to the raw option-id when unknown
}