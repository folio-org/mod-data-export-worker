package org.folio.dew.batch.acquisitions.edifact.services;

import org.folio.dew.batch.acquisitions.edifact.client.LocationClient;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LocationService {
  @Autowired
  private final LocationClient locationClient;

  private JSONObject getLocation(String id) {
    return locationClient.getLocation(id);
  }

  @Cacheable(cacheNames = "locations")
  public String getLocationCode(String id) {
    JSONObject jsonObject = getLocation(id);
    String locationCode = "";

    if (!jsonObject.isEmpty() && jsonObject.getString("code") != null) {
      locationCode = jsonObject.getString("code");
    }

    return locationCode;
  }
}
