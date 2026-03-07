package org.folio.dew.batch.acquisitions.services;

import org.apache.commons.lang3.StringUtils;
import org.folio.dew.client.LocationClient;
import org.folio.dew.domain.dto.acquisitions.edifact.Location;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.folio.spring.utils.FolioExecutionContextUtils;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import static java.util.Optional.ofNullable;

@Service
@RequiredArgsConstructor
public class LocationService {
  private final LocationClient locationClient;
  private final FolioModuleMetadata folioModuleMetadata;
  private final FolioExecutionContext folioExecutionContext;

  private Location getLocation(String id) {
    return locationClient.getLocation(id);
  }

  public String getLocationCodeById(String locationId, String tenantId) {
    Location location;
    if (StringUtils.isNotBlank(tenantId)) {
      try (var ignored = new FolioExecutionContextSetter(
        FolioExecutionContextUtils.prepareContextForTenant(tenantId, folioModuleMetadata, folioExecutionContext))) {

        location = getLocation(locationId);
      }
    } else {
      location = getLocation(locationId);
    }

    String locationCode = "";

    if (location != null) {
      locationCode = ofNullable(location.getCode()).orElse("");
    }

    return locationCode;
  }
}
