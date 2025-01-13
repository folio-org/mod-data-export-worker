package org.folio.dew.batch.acquisitions.services;

import org.apache.commons.lang3.StringUtils;
import org.folio.dew.client.HoldingClient;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

import java.util.Objects;

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

  public String getInstanceIdByHolding(JsonNode holding) {
    return Objects.isNull(holding) || holding.isEmpty() ? StringUtils.EMPTY : holding.get("instanceId").asText();
  }
}
