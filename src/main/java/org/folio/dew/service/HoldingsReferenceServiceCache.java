package org.folio.dew.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;
import org.folio.dew.client.CallNumberTypeClient;
import org.folio.dew.client.HoldingClient;
import org.folio.dew.client.HoldingsNoteTypeClient;
import org.folio.dew.client.HoldingsSourceClient;
import org.folio.dew.client.HoldingsTypeClient;
import org.folio.dew.client.IllPolicyClient;
import org.folio.dew.client.LocationClient;
import org.folio.dew.client.StatisticalCodeClient;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.error.NotFoundException;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;


@Service
@RequiredArgsConstructor
@Log4j2
public class HoldingsReferenceServiceCache extends FolioExecutionContextManager {

  private final HoldingsTypeClient holdingsTypeClient;
  private final LocationClient locationClient;
  private final CallNumberTypeClient callNumberTypeClient;
  private final HoldingsNoteTypeClient holdingsNoteTypeClient;
  private final IllPolicyClient illPolicyClient;
  private final HoldingsSourceClient sourceClient;
  private final StatisticalCodeClient statisticalCodeClient;
  private final HoldingClient holdingClient;
  private final FolioExecutionContext folioExecutionContext;


  @Cacheable(cacheNames = "holdingsTypesNames")
  public String getHoldingsTypeNameById(String id, String tenantId) {
   if (isEmpty(id)) {
      return EMPTY;
    }
    try (var context = new FolioExecutionContextSetter(refreshAndGetFolioExecutionContext(tenantId, folioExecutionContext))) {
      return holdingsTypeClient.getById(id).getName();
    }
  }

  @Cacheable(cacheNames = "holdingsLocationsNames")
  public String getLocationNameById(String id, String tenantId) {
    try (var context = new FolioExecutionContextSetter(refreshAndGetFolioExecutionContext(tenantId, folioExecutionContext))) {
      return isEmpty(id) ? EMPTY : locationClient.getLocationById(id).getName();
    } catch (NotFoundException e) {
      var msg = "Location not found by id=" + id;
      log.error(msg);
      throw new BulkEditException(msg);
    }
  }

  @Cacheable(cacheNames = "holdingsCallNumberTypesNames")
  public String getCallNumberTypeNameById(String id, String tenantId) {
    if (isEmpty(id)) {
      return EMPTY;
    }
    try (var context = new FolioExecutionContextSetter(refreshAndGetFolioExecutionContext(tenantId, folioExecutionContext))) {
      return callNumberTypeClient.getById(id).getName();
    }
  }

  @Cacheable(cacheNames = "holdingsNoteTypesNames")
  public String getNoteTypeNameById(String id, String tenantId) {
    if (isEmpty(id)) {
      return EMPTY;
    }
    try (var context = new FolioExecutionContextSetter(refreshAndGetFolioExecutionContext(tenantId, folioExecutionContext))) {
      return holdingsNoteTypeClient.getById(id).getName();
    }
  }

  @Cacheable(cacheNames = "illPolicyNames")
  public String getIllPolicyNameById(String id, String tenantId) {
    if (isEmpty(id)) {
      return EMPTY;
    }
    try (var context = new FolioExecutionContextSetter(refreshAndGetFolioExecutionContext(tenantId, folioExecutionContext))) {
      return illPolicyClient.getById(id).getName();
    }
  }

  @Cacheable(cacheNames = "holdingsSourceNames")
  public String getSourceNameById(String id, String tenantId) {
    if (isEmpty(id)) {
      return EMPTY;
    }
    try (var context = new FolioExecutionContextSetter(refreshAndGetFolioExecutionContext(tenantId, folioExecutionContext))) {
      return sourceClient.getById(id).getName();
    }
  }

  @Cacheable(cacheNames = "holdingsStatisticalCodeNames")
  public String getStatisticalCodeNameById(String id, String tenantId) {
    if (isEmpty(id)) {
      return EMPTY;
    }
    try (var context = new FolioExecutionContextSetter(refreshAndGetFolioExecutionContext(tenantId, folioExecutionContext))) {
      return statisticalCodeClient.getById(id).getName();
    }
  }

  @Cacheable(cacheNames = "holdings")
  public JsonNode getHoldingsJsonById(String holdingsId, String tenantId) {
    try (var context = new FolioExecutionContextSetter(refreshAndGetFolioExecutionContext(tenantId, folioExecutionContext))) {
      return holdingClient.getHoldingById(holdingsId);
    }
  }

  @Cacheable(cacheNames = "holdingsLocations")
  public JsonNode getHoldingsLocationById(String locationId, String tenantId) {
    if (ObjectUtils.isEmpty(locationId)) {
      return new ObjectMapper().createObjectNode();
    }
    try (var context = new FolioExecutionContextSetter(refreshAndGetFolioExecutionContext(tenantId, folioExecutionContext))) {
      return locationClient.getLocation(locationId);
    }
  }

}
