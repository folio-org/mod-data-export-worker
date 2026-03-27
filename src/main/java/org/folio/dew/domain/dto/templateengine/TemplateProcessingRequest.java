package org.folio.dew.domain.dto.templateengine;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class TemplateProcessingRequest {

  private UUID templateId;
  @Builder.Default
  private String lang = "en";
  @Builder.Default
  private String outputFormat = "text/html";
}