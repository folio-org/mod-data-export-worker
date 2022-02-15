package org.folio.dew.batch.acquisitions.edifact.services;

import org.folio.dew.client.LocationClient;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LocationService {
  private final LocationClient locationClient;

  private JsonNode getLocation(String id) {
    return locationClient.getLocation(id);
  }

  public String getLocationCodeById(String id) {
    JsonNode jsonObject = getLocation(id);
    String locationCode = "";

    if (jsonObject != null && !jsonObject.isEmpty()) {
      locationCode = jsonObject.get("code").asText();
    }

    return locationCode;
  }
}
