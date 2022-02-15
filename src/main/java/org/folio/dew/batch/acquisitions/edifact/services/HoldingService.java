package org.folio.dew.batch.acquisitions.edifact.services;

import org.folio.dew.client.HoldingClient;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HoldingService {
  private final HoldingClient holdingClient;

  public JsonNode getHoldingById(String id) {
    return holdingClient.getHoldingById(id);
  }

  public String getPermanentLocationByHoldingId(String id) {
    JsonNode jsonObject = getHoldingById(id);
    String locationId = "";

    if (jsonObject != null && !jsonObject.isEmpty()) {
      locationId = jsonObject.get("permanentLocationId").asText();
    }

    return locationId;
  }
}
