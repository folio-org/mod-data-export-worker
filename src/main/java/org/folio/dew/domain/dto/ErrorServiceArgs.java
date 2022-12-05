package org.folio.dew.domain.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ErrorServiceArgs {
  private final String jobId;
  private final String identifier;
  private final String fileName;
}
