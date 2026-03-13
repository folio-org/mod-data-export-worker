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
  private String outputFormat = "text/plain";
  private ClaimsContext context;

  @Data
  @Builder
  public static class ClaimsContext {
    private String configName;
    private String fileName;
    private String exportDate;
    private int pieceCount;
  }

}