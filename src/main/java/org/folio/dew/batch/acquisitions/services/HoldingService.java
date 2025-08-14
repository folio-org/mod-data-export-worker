package org.folio.dew.batch.acquisitions.services;

import org.apache.commons.lang3.StringUtils;
import org.folio.dew.client.HoldingClient;
import org.folio.dew.domain.dto.Location;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.folio.spring.utils.FolioExecutionContextUtils;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class HoldingService {
  private final HoldingClient holdingClient;
  private final FolioModuleMetadata folioModuleMetadata;
  private final FolioExecutionContext folioExecutionContext;

  public JsonNode getHoldingById(String id) {
    return holdingClient.getHoldingById(id);
  }

  public String getPermanentLocationByHoldingId(Location location) {
    JsonNode jsonObject;
    String holdingId = location.getHoldingId().toString();
    if (StringUtils.isNotBlank(location.getTenantId())) {
      try (var ignored = new FolioExecutionContextSetter(
        FolioExecutionContextUtils.prepareContextForTenant(location.getTenantId(), folioModuleMetadata, folioExecutionContext))) {

        jsonObject = getHoldingById(holdingId);
      }
    } else {
      jsonObject = getHoldingById(holdingId);
    }

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
