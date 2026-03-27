package org.folio.dew.domain.dto.templateengine;

import lombok.Data;

@Data
public class TemplateProcessingResponse {

  private Result result;
  private Meta meta;

  @Data
  public static class Result {
    private String header;
    private String body;
  }

  @Data
  public static class Meta {
    private String outputFormat;
  }
}