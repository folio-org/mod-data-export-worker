package org.folio.dew.batch.acquisitions.services;

import org.folio.dew.client.IdentifierTypeClient;
import org.folio.dew.domain.dto.acquisitions.edifact.IdentifierType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IdentifierTypeService {
  @Autowired
  private final IdentifierTypeClient identifierTypeClient;

  private IdentifierType getIdentifierType(String id) {
    return identifierTypeClient.getIdentifierType(id);
  }

  @Cacheable(cacheNames = "identifierTypes")
  public String getIdentifierTypeName(String id) {
    IdentifierType identifierType = getIdentifierType(id);
    String name = "";

    if (identifierType != null) {
      name = identifierType.getName();
    }

    return name;
  }
}
