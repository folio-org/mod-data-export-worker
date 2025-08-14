package org.folio.dew.batch.acquisitions.services;

import org.apache.commons.lang3.StringUtils;
import org.folio.dew.client.LocationClient;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.folio.spring.utils.FolioExecutionContextUtils;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LocationService {
  private final LocationClient locationClient;
  private final FolioModuleMetadata folioModuleMetadata;
  private final FolioExecutionContext folioExecutionContext;

  private JsonNode getLocation(String id) {
    return locationClient.getLocation(id);
  }

  public String getLocationCodeById(String locationId, String tenantId) {
    JsonNode jsonObject;
    if (StringUtils.isNotBlank(tenantId)) {
      try (var ignored = new FolioExecutionContextSetter(
        FolioExecutionContextUtils.prepareContextForTenant(tenantId, folioModuleMetadata, folioExecutionContext))) {

        jsonObject = getLocation(locationId);
      }
    } else {
      jsonObject = getLocation(locationId);
    }

    String locationCode = "";

    if (jsonObject != null && !jsonObject.isEmpty()) {
      locationCode = jsonObject.get("code").asText();
    }

    return locationCode;
  }
}
