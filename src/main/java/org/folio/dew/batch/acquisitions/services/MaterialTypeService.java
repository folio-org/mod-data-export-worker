package org.folio.dew.batch.acquisitions.services;

import org.folio.dew.client.MaterialTypeClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MaterialTypeService {
  private final MaterialTypeClient materialTypeClient;

  private JsonNode getMaterialType(String id) {
    return materialTypeClient.getMaterialType(id);
  }

  @Cacheable(cacheNames = "materialTypeNames")
  public String getMaterialTypeName(String id) {
    JsonNode jsonObject = getMaterialType(id);
    String materialType = "";

    if (jsonObject != null && !jsonObject.isEmpty()) {
      materialType = jsonObject.get("name").asText();
    }

    return materialType;
  }
}
