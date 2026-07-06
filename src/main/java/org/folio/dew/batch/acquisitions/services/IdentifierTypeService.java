package org.folio.dew.batch.acquisitions.services;

import org.folio.dew.client.IdentifierTypeClient;
import org.folio.dew.domain.dto.acquisitions.edifact.IdentifierType;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IdentifierTypeService {

  private final IdentifierTypeClient identifierTypeClient;

  @Cacheable(cacheNames = "identifierTypes", unless = "#result == null")
  public String getIdentifierTypeName(String id) {
    IdentifierType identifierType = identifierTypeClient.getIdentifierType(id);
    return identifierType != null ? identifierType.getName() : "";
  }
}
