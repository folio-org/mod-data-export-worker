package org.folio.dew.batch.acquisitions.edifact.services;

import org.folio.dew.batch.acquisitions.edifact.client.HoldingClient;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HoldingService {
  @Autowired
  private final HoldingClient holdingClient;

  private JSONObject getHolding(String id) {
    return holdingClient.getHolding(id);
  }

  @Cacheable(cacheNames = "holdings")
  public String getPermanentLocationId(String id) {
    JSONObject jsonObject = getHolding(id);
    String locationId = "";

    if (!jsonObject.isEmpty() && jsonObject.getString("permanentLocationId") != null) {
      locationId = jsonObject.getString("permanentLocationId");
    }

    return locationId;
  }
}
