package org.folio.dew.batch.acquisitions.edifact.services;

import org.folio.dew.client.IdentifierTypeClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IdentifierTypeService {
  @Autowired
  private final IdentifierTypeClient identifierTypeClient;

  private JsonNode getIdentifierType(String id) {
    return identifierTypeClient.getIdentifierType(id);
  }

  @Cacheable(cacheNames = "identifierTypes")
  public String getIdentifierTypeName(String id) {
    JsonNode jsonObject = getIdentifierType(id);
    String identifierType = "";

    if (jsonObject != null && !jsonObject.isEmpty()) {
      identifierType = jsonObject.get("name").asText();
    }

    return identifierType;
  }
}
