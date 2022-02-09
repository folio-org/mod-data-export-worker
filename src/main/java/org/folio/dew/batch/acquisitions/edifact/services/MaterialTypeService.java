package org.folio.dew.batch.acquisitions.edifact.services;

import lombok.RequiredArgsConstructor;
import org.folio.dew.client.MaterialTypeClient;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MaterialTypeService {
  @Autowired
  private final MaterialTypeClient materialTypeClient;

  private JSONObject getMaterialType(String id) {
    return materialTypeClient.getMaterialType(id);
  }

  @Cacheable(cacheNames = "materialTypes")
  public String getMaterialTypeName(String id) {
    JSONObject jsonObject = getMaterialType(id);
    String materialType = "";

    if (!jsonObject.isEmpty() && jsonObject.getString("name") != null) {
      materialType = jsonObject.getString("name");
    }

    return materialType;
  }
}
