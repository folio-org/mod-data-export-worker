package org.folio.dew.domain.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ReferenceServiceArgs<T> {
  private final String jobId;
  private final String identifierType;
  private final String fileName;
  private final T entity;
}
