package org.folio.dew.service;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.client.CallNumberTypeClient;
import org.folio.dew.client.HoldingsNoteTypeClient;
import org.folio.dew.client.HoldingsSourceClient;
import org.folio.dew.client.HoldingsTypeClient;
import org.folio.dew.client.IllPolicyClient;
import org.folio.dew.client.InstanceClient;
import org.folio.dew.client.InventoryClient;
import org.folio.dew.client.LocationClient;
import org.folio.dew.client.StatisticalCodeClient;
import org.folio.dew.domain.dto.IllPolicy;
import org.folio.dew.domain.dto.ItemLocation;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.error.NotFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class HoldingsReferenceService {
  private static final String QUERY_PATTERN_NAME = "name==\"%s\"";
  private static final String QUERY_PATTERN_HRID = "hrid==\"%s\"";
  private static final String QUERY_PATTERN_BARCODE = "barcode==\"%s\"";

  private final InstanceClient instanceClient;
  private final InventoryClient inventoryClient;
  private final HoldingsTypeClient holdingsTypeClient;
  private final LocationClient locationClient;
  private final CallNumberTypeClient callNumberTypeClient;
  private final HoldingsNoteTypeClient holdingsNoteTypeClient;
  private final IllPolicyClient illPolicyClient;
  private final HoldingsSourceClient sourceClient;
  private final StatisticalCodeClient statisticalCodeClient;

  public String getInstanceIdByHrid(String instanceHrid) {
    var briefInstances = instanceClient.getByQuery(String.format(QUERY_PATTERN_HRID, instanceHrid));
    if (briefInstances.getInstances().isEmpty()) {
      throw new BulkEditException("Instance not found by hrid=" + instanceHrid);
    } else {
      return briefInstances.getInstances().get(0).getId();
    }
  }

  public String getInstanceTitleById(String instanceId) {
    try {
      return isEmpty(instanceId) ? EMPTY : instanceClient.getById(instanceId).getTitle();
    } catch (NotFoundException e) {
      var msg = "Instance not found by id=" + instanceId;
      log.error(msg);
      throw new BulkEditException(msg);
    }
  }

  public String getHoldingsIdByItemBarcode(String itemBarcode) {
    var items = inventoryClient.getItemByQuery(String.format(QUERY_PATTERN_BARCODE, itemBarcode), 1);
    if (items.getItems().isEmpty()) {
      throw new BulkEditException("Item not found by barcode=" + itemBarcode);
    }
    return items.getItems().get(0).getHoldingsRecordId();
  }

  @Cacheable(cacheNames = "holdingsTypesNames")
  public String getHoldingsTypeNameById(String id) {
    try {
      return isEmpty(id) ? EMPTY : holdingsTypeClient.getById(id).getName();
    } catch (NotFoundException e) {
      var msg = "Holdings type not found by id=" + id;
      log.error(msg);
      throw new BulkEditException(msg);
    }
  }

  @Cacheable(cacheNames = "holdingsTypes")
  public String getHoldingsTypeIdByName(String name) {
    var holdingsTypes = holdingsTypeClient.getByQuery(String.format(QUERY_PATTERN_NAME, name));
    if (holdingsTypes.getHoldingsTypes().isEmpty()) {
      var msg = "Holdings type not found by name=" + name;
      log.error(msg);
      throw new BulkEditException(msg);
    }
    return holdingsTypes.getHoldingsTypes().get(0).getId();
  }

  @Cacheable(cacheNames = "holdingsLocationsNames")
  public String getLocationNameById(String id) {
    try {
      return isEmpty(id) ? EMPTY : locationClient.getLocationById(id).getName();
    } catch (NotFoundException e) {
      var msg = "Location not found by id=" + id;
      log.error(msg);
      throw new BulkEditException(msg);
    }
  }

  @Cacheable(cacheNames = "holdingsLocations")
  public ItemLocation getLocationByName(String name) {
    var locations = locationClient.getLocationByQuery(String.format(QUERY_PATTERN_NAME, name));
    if (locations.getLocations().isEmpty()) {
      var msg = "Location not found by name=" + name;
      log.error(msg);
      throw new BulkEditException(msg);
    }
    return locations.getLocations().get(0);
  }

  @Cacheable(cacheNames = "holdingsCallNumberTypesNames")
  public String getCallNumberTypeNameById(String id) {
    try {
      return isEmpty(id) ? EMPTY : callNumberTypeClient.getById(id).getName();
    } catch (NotFoundException e) {
      var msg = "Call number type not found by id=" + id;
      log.error(msg);
      throw new BulkEditException(msg);
    }
  }

  @Cacheable(cacheNames = "holdingsCallNumberTypes")
  public String getCallNumberTypeIdByName(String name) {
    var callNumberTypes = callNumberTypeClient.getByQuery(String.format(QUERY_PATTERN_NAME, name));
    if (callNumberTypes.getCallNumberTypes().isEmpty()) {
      var msg = "Call number type not found by name=" + name;
      log.error(msg);
      throw new BulkEditException(msg);
    }
    return callNumberTypes.getCallNumberTypes().get(0).getId();
  }

  @Cacheable(cacheNames = "holdingsNoteTypesNames")
  public String getNoteTypeNameById(String id) {
    try {
      return isEmpty(id) ? EMPTY : holdingsNoteTypeClient.getById(id).getName();
    } catch (NotFoundException e) {
      var msg = "Note type not found by id=" + id;
      log.error(msg);
      throw new BulkEditException(msg);
    }
  }

  @Cacheable(cacheNames = "holdingsNoteTypes")
  public String getNoteTypeIdByName(String name) {
    var noteTypes = holdingsNoteTypeClient.getByQuery(String.format(QUERY_PATTERN_NAME, name));
    if (noteTypes.getHoldingsNoteTypes().isEmpty()) {
      var msg = "Note type not found by name=" + name;
      log.error(msg);
      throw new BulkEditException(msg);
    }
    return noteTypes.getHoldingsNoteTypes().get(0).getId();
  }

  @Cacheable(cacheNames = "illPolicyNames")
  public String getIllPolicyNameById(String id) {
    try {
      return isEmpty(id) ? EMPTY : illPolicyClient.getById(id).getName();
    } catch (NotFoundException e) {
      var msg = "Ill policy not found by id=" + id;
      log.error(msg);
      throw new BulkEditException(msg);
    }
  }

  @Cacheable(cacheNames = "illPolicies")
  public IllPolicy getIllPolicyByName(String name) {
    var illPolicies = illPolicyClient.getByQuery(String.format(QUERY_PATTERN_NAME, name));
    if (illPolicies.getIllPolicies().isEmpty()) {
      var msg = "Ill policy not found by name=" + name;
      log.error(msg);
      throw new BulkEditException(msg);
    }
    return illPolicies.getIllPolicies().get(0);
  }

  @Cacheable(cacheNames = "holdingsSourceNames")
  public String getSourceNameById(String id) {
    try {
      return isEmpty(id) ? EMPTY : sourceClient.getById(id).getName();
    } catch (NotFoundException e) {
      var msg = "Holdings record source not found by id=" + id;
      log.error(msg);
      throw new BulkEditException(msg);
    }
  }

  @Cacheable(cacheNames = "holdingsSources")
  public String getSourceIdByName(String name) {
    var sources = sourceClient.getByQuery(String.format(QUERY_PATTERN_NAME, name));
    if (sources.getHoldingsRecordsSources().isEmpty()) {
      var msg = "Source not found by name=" + name;
      log.error(msg);
      throw new BulkEditException(msg);
    }
    return sources.getHoldingsRecordsSources().get(0).getId();
  }

  @Cacheable(cacheNames = "holdingsStatisticalCodeNames")
  public String getStatisticalCodeNameById(String id) {
    try {
      return isEmpty(id) ? EMPTY : statisticalCodeClient.getById(id).getName();
    } catch (NotFoundException e) {
      var msg = "Statistical code not found by id=" + id;
      log.error(msg);
      throw new BulkEditException(msg);
    }
  }

  @Cacheable(cacheNames = "holdingsStatisticalCodes")
  public String getStatisticalCodeIdByName(String name) {
    var statisticalCodes = statisticalCodeClient.getByQuery(String.format(QUERY_PATTERN_NAME, name));
    if (statisticalCodes.getStatisticalCodes().isEmpty()) {
      var msg = "Statistical code not found by name=" + name;
      log.error(msg);
      throw new BulkEditException(msg);
    }
    return statisticalCodes.getStatisticalCodes().get(0).getId();
  }
}
