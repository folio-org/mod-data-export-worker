package org.folio.dew.batch.acquisitions.edifact.services;

import org.folio.dew.batch.acquisitions.edifact.client.IdentifierTypeClient;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IdentifierTypeService {
  @Autowired
  private final IdentifierTypeClient identifierTypeClient;

  private JSONObject getIdentifierType(String id) {
    return identifierTypeClient.getIdentifierType(id);
  }

  @Cacheable(cacheNames = "identifier-types")
  public String getIdentifierTypeName(String id) {
    JSONObject jsonObject = getIdentifierType(id);
    String identifierType = "";

    if (!jsonObject.isEmpty() && jsonObject.getString("name") != null) {
      identifierType = jsonObject.getString("name");
    }

    return identifierType;
  }
}
