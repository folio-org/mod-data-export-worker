package org.folio.dew.service;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.dew.utils.Constants.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.client.CallNumberTypeClient;
import org.folio.dew.client.ConfigurationClient;
import org.folio.dew.client.DamagedStatusClient;
import org.folio.dew.client.HoldingClient;
import org.folio.dew.client.ItemNoteTypeClient;
import org.folio.dew.client.LoanTypeClient;
import org.folio.dew.client.LocationClient;
import org.folio.dew.client.MaterialTypeClient;
import org.folio.dew.client.ServicePointClient;
import org.folio.dew.client.StatisticalCodeClient;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.ErrorServiceArgs;
import org.folio.dew.domain.dto.ItemLocation;
import org.folio.dew.domain.dto.ItemLocationCollection;
import org.folio.dew.domain.dto.LoanType;
import org.folio.dew.domain.dto.LoanTypeCollection;
import org.folio.dew.domain.dto.MaterialType;
import org.folio.dew.domain.dto.MaterialTypeCollection;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.error.ConfigurationException;
import org.folio.dew.error.NotFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@Service
@Log4j2
@RequiredArgsConstructor
public class ItemReferenceService {
  private static final String QUERY_PATTERN_NAME = "name==\"%s\"";
  private static final String QUERY_PATTERN_CODE = "code==\"%s\"";
  private static final String QUERY_PATTERN_USERNAME = "username==\"%s\"";

  private final CallNumberTypeClient callNumberTypeClient;
  private final DamagedStatusClient damagedStatusClient;
  private final ItemNoteTypeClient itemNoteTypeClient;
  private final ServicePointClient servicePointClient;
  private final StatisticalCodeClient statisticalCodeClient;
  private final UserClient userClient;
  private final LocationClient locationClient;
  private final MaterialTypeClient materialTypeClient;
  private final HoldingClient holdingClient;
  private final LoanTypeClient loanTypeClient;
  private final ConfigurationClient configurationClient;
  private final ObjectMapper objectMapper;
  private final BulkEditProcessingErrorsService errorsService;

  @Cacheable(cacheNames = "callNumberTypeNames")
  public String getCallNumberTypeNameById(String callNumberTypeId, ErrorServiceArgs args) {
    try {
      return isEmpty(callNumberTypeId) ? EMPTY : callNumberTypeClient.getById(callNumberTypeId).getName();
    } catch (NotFoundException e) {
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(String.format("Call number type was not found by id: [%s]", callNumberTypeId)), args.getFileName());
      return callNumberTypeId;
    }
  }

  @Cacheable(cacheNames = "callNumberTypeIds")
  public String getCallNumberTypeIdByName(String name) {
    if (isEmpty(name)) {
      return null;
    }
    var response = callNumberTypeClient.getByQuery(String.format(QUERY_PATTERN_NAME, name));
    if (response.getCallNumberTypes().isEmpty()) {
      return name;
    }
    return response.getCallNumberTypes().get(0).getId();
  }

  @Cacheable(cacheNames = "damagedStatusNames")
  public String getDamagedStatusNameById(String damagedStatusId, ErrorServiceArgs args) {
    try {
      return isEmpty(damagedStatusId) ? EMPTY : damagedStatusClient.getById(damagedStatusId).getName();
    } catch (NotFoundException e) {
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(String.format("Damaged status was not found by id: [%s]", damagedStatusId)), args.getFileName());
      return damagedStatusId;
    }
  }

  @Cacheable(cacheNames = "damagedStatusIds")
  public String getDamagedStatusIdByName(String name) {
    if (isEmpty(name)) {
      return null;
    }
    var response = damagedStatusClient.getByQuery(String.format(QUERY_PATTERN_NAME, name));
    if (response.getItemDamageStatuses().isEmpty()) {
      return name;
    }
    return response.getItemDamageStatuses().get(0).getId();
  }

  @Cacheable(cacheNames = "noteTypeNames")
  public String getNoteTypeNameById(String noteTypeId, ErrorServiceArgs args) {
    try {
      return isEmpty(noteTypeId) ? EMPTY : itemNoteTypeClient.getById(noteTypeId).getName();
    } catch (NotFoundException e) {
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(String.format("Note type was not found by id: [%s]", noteTypeId)), args.getFileName());
      return noteTypeId;
    }
  }

  @Cacheable(cacheNames = "noteTypeIds")
  public String getNoteTypeIdByName(String name) {
    if (isEmpty(name)) {
      return null;
    }
    var response = itemNoteTypeClient.getByQuery(String.format(QUERY_PATTERN_NAME, name));
    if (response.getItemNoteTypes().isEmpty()) {
      return name;
    }
    return response.getItemNoteTypes().get(0).getId();
  }

  @Cacheable(cacheNames = "servicePointNames")
  public String getServicePointNameById(String servicePointId, ErrorServiceArgs args) {
    try {
      return isEmpty(servicePointId) ? EMPTY : servicePointClient.getById(servicePointId).getName();
    } catch (NotFoundException e) {
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(String.format("Service point was not found by id: [%s]", servicePointId)), args.getFileName());
      return servicePointId;
    }
  }

  @Cacheable(cacheNames = "servicePointIds")
  public String getServicePointIdByName(String name) {
    if (isEmpty(name)) {
      return null;
    }
    var response = servicePointClient.get(String.format(QUERY_PATTERN_NAME, name), 1L);
    if (response.getServicepoints().isEmpty()) {
      return name;
    }
    return response.getServicepoints().get(0).getId();
  }

  @Cacheable(cacheNames = "statisticalCodeNames")
  public String getStatisticalCodeById(String statisticalCodeId, ErrorServiceArgs args) {
    try {
      return isEmpty(statisticalCodeId) ? EMPTY : statisticalCodeClient.getById(statisticalCodeId).getCode();
    } catch (NotFoundException e) {
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(String.format("Statistical code was not found by id: [%s]", statisticalCodeId)), args.getFileName());
      return statisticalCodeId;
    }
  }

  @Cacheable(cacheNames = "statisticalCodeIds")
  public String getStatisticalCodeIdByCode(String code) {
    if (isEmpty(code)) {
      return null;
    }
    var response = statisticalCodeClient.getByQuery(String.format(QUERY_PATTERN_CODE, code));
    if (response.getStatisticalCodes().isEmpty()) {
      return code;
    }
    return response.getStatisticalCodes().get(0).getId();
  }

  @Cacheable(cacheNames = "userNames")
  public String getUserNameById(String userId, ErrorServiceArgs args) {
    try {
      return isEmpty(userId) ? EMPTY : userClient.getUserById(userId).getUsername();
    } catch (NotFoundException e) {
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(String.format("User name was not found by id: [%s]", userId)), args.getFileName());
      return userId;
    }
  }

  @Cacheable(cacheNames = "userIds")
  public String getUserIdByUserName(String name) {
    if (isEmpty(name)) {
      return null;
    }
    var response = userClient.getUserByQuery(String.format(QUERY_PATTERN_USERNAME, name));
    if (response.getUsers().isEmpty()) {
      return name;
    }
    return response.getUsers().get(0).getId();
  }

  @Cacheable(cacheNames = "locations")
  public ItemLocationCollection getItemLocationsByName(String name) {
    return locationClient.getLocationByQuery(String.format(QUERY_PATTERN_NAME, name));
  }

  public ItemLocation getLocationByName(String name) {
    var locations = getItemLocationsByName(name);
    if (locations.getLocations().isEmpty()) {
      throw new BulkEditException("Location not found: " + name);
    }
    return locations.getLocations().get(0);
  }

  @Cacheable(cacheNames = "materialTypes")
  public MaterialTypeCollection getMaterialTypesByName(String name) {
    return materialTypeClient.getByQuery(String.format(QUERY_PATTERN_NAME, name));
  }

  public MaterialType getMaterialTypeByName(String name) {
    var types = getMaterialTypesByName(name);
    if (types.getMtypes().isEmpty()) {
      throw new BulkEditException("Material type not found: " + name);
    }
    return types.getMtypes().get(0);
  }

  @Cacheable(cacheNames = "loanTypes")
  public LoanTypeCollection getLoanTypesByName(String name) {
    return loanTypeClient.getByQuery(String.format(QUERY_PATTERN_NAME, name));
  }

  public LoanType getLoanTypeByName(String name) {
    var loanTypes = getLoanTypesByName(name);
    if (loanTypes.getLoantypes().isEmpty()) {
      throw new BulkEditException("Loan type not found: " + name);
    }
    return loanTypes.getLoantypes().get(0);
  }

  @Cacheable(cacheNames = "holdings")
  public String getHoldingEffectiveLocationCodeById(String id) {
    var holdingJson = holdingClient.getHoldingById(id);
    var effectiveLocationId = isEmpty(holdingJson.get("effectiveLocationId")) ? getHoldingsEffectiveLocation(holdingJson) : holdingJson.get("effectiveLocationId");
    if (nonNull(effectiveLocationId)) {
      var locationJson = locationClient.getLocation(effectiveLocationId.asText());
      return isEmpty(locationJson.get("name")) ? EMPTY : locationJson.get("name").asText();
    }
    return EMPTY;
  }

  @Cacheable(cacheNames = "allowedStatuses")
  public List<String> getAllowedStatuses(String statusName) {
    var configurations = configurationClient.getConfigurations(String.format(BULK_EDIT_CONFIGURATIONS_QUERY_TEMPLATE, MODULE_NAME, STATUSES_CONFIG_NAME));
    if (configurations.getConfigs().isEmpty()) {
      throw new NotFoundException("Statuses configuration was not found");
    }
    try {
      var statuses = objectMapper
        .readValue(configurations.getConfigs().get(0).getValue(), new TypeReference<HashMap<String, List<String>>>() {});
      return statuses.getOrDefault(statusName, Collections.emptyList());
    } catch (JsonProcessingException e) {
      var msg = String.format("Error reading configuration, reason: %s", e.getMessage());
      log.error(msg);
      throw new ConfigurationException(msg);
    }
  }

  private JsonNode getHoldingsEffectiveLocation(JsonNode holdingsJson) {
    return isEmpty(holdingsJson.get("temporaryLocationId")) ? holdingsJson.get("permanentLocationId") : holdingsJson.get("temporaryLocationId");
  }

  public String getEffectiveLocationCallNumberComponentsForItem(String holdingsRecordId){
    if(StringUtils.isEmpty(holdingsRecordId)){
      return EMPTY;
    }

    var holdingJson = holdingClient.getHoldingById(holdingsRecordId);
    var effectiveLocationId = isEmpty(holdingJson.get("effectiveLocationId")) ? getHoldingsEffectiveLocation(holdingJson) : holdingJson.get("effectiveLocationId");
    var effectiveLocationName = EMPTY;
    if (nonNull(effectiveLocationId)) {
      var locationJson = locationClient.getLocation(effectiveLocationId.asText());
      effectiveLocationName = isEmpty(locationJson.get("name")) ? EMPTY : locationJson.get("name").asText();
    }

    var callNumber = isEmpty(holdingJson.get("callNumber")) ? EMPTY : holdingJson.get("callNumber").asText();

    if(StringUtils.isEmpty(effectiveLocationName) && StringUtils.isEmpty(callNumber)){
      return EMPTY;
    }

    return String.join(HOLDINGS_LOCATION_CALL_NUMBER_DELIMITER, effectiveLocationName, callNumber);
  }

}
