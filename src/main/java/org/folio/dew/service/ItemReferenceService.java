package org.folio.dew.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.client.CallNumberTypeClient;
import org.folio.dew.client.ConfigurationClient;
import org.folio.dew.client.DamagedStatusClient;
import org.folio.dew.client.ElectronicAccessRelationshipClient;
import org.folio.dew.client.HoldingClient;
import org.folio.dew.client.InstanceClient;
import org.folio.dew.client.ItemNoteTypeClient;
import org.folio.dew.client.LoanTypeClient;
import org.folio.dew.client.LocationClient;
import org.folio.dew.client.MaterialTypeClient;
import org.folio.dew.client.ServicePointClient;
import org.folio.dew.client.StatisticalCodeClient;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.BriefHoldingsRecord;
import org.folio.dew.domain.dto.BriefHoldingsRecordCollection;
import org.folio.dew.domain.dto.BriefInstance;
import org.folio.dew.domain.dto.BriefInstanceCollection;
import org.folio.dew.domain.dto.CallNumberType;
import org.folio.dew.domain.dto.CallNumberTypeCollection;
import org.folio.dew.domain.dto.DamagedStatus;
import org.folio.dew.domain.dto.DamagedStatusCollection;
import org.folio.dew.domain.dto.ElectronicAccessRelationship;
import org.folio.dew.domain.dto.ElectronicAccessRelationshipCollection;
import org.folio.dew.domain.dto.ItemLocation;
import org.folio.dew.domain.dto.ItemLocationCollection;
import org.folio.dew.domain.dto.LoanType;
import org.folio.dew.domain.dto.LoanTypeCollection;
import org.folio.dew.domain.dto.MaterialType;
import org.folio.dew.domain.dto.MaterialTypeCollection;
import org.folio.dew.domain.dto.NoteType;
import org.folio.dew.domain.dto.NoteTypeCollection;
import org.folio.dew.domain.dto.ServicePoint;
import org.folio.dew.domain.dto.ServicePoints;
import org.folio.dew.domain.dto.StatisticalCode;
import org.folio.dew.domain.dto.StatisticalCodeCollection;
import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.UserCollection;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.error.ConfigurationException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.folio.dew.utils.Constants.BULK_EDIT_CONFIGURATIONS_QUERY_TEMPLATE;
import static org.folio.dew.utils.Constants.MODULE_NAME;
import static org.folio.dew.utils.Constants.STATUSES_CONFIG_NAME;

@Service
@Log4j2
@RequiredArgsConstructor
public class ItemReferenceService {
  private static final String NAME = "name==";
  private static final String HRID = "hrid==";
  private static final String CODE = "code==";
  private static final String USERNAME = "username==";

  private final CallNumberTypeClient callNumberTypeClient;
  private final DamagedStatusClient damagedStatusClient;
  private final ItemNoteTypeClient itemNoteTypeClient;
  private final ElectronicAccessRelationshipClient relationshipClient;
  private final ServicePointClient servicePointClient;
  private final StatisticalCodeClient statisticalCodeClient;
  private final UserClient userClient;
  private final LocationClient locationClient;
  private final MaterialTypeClient materialTypeClient;
  private final HoldingClient holdingClient;
  private final InstanceClient instanceClient;
  private final LoanTypeClient loanTypeClient;
  private final ConfigurationClient configurationClient;

  @Cacheable(cacheNames = "callNumberTypes")
  public CallNumberType getCallNumberTypeById(String id) {
    return callNumberTypeClient.getById(id);
  }

  @Cacheable(cacheNames = "callNumberTypes")
  public CallNumberTypeCollection getCallNumberTypesByName(String name) {
    return callNumberTypeClient.getByQuery(NAME + name);
  }

  public CallNumberType getCallNumberTypeByName(String name) {
    var callNumberTypes = getCallNumberTypesByName(name);
    if (callNumberTypes.getCallNumberTypes().isEmpty()) {
      throw new BulkEditException("Call number type not found: " + name);
    }
    return callNumberTypes.getCallNumberTypes().get(0);
  }

  @Cacheable(cacheNames = "damagedStatuses")
  public DamagedStatus getDamagedStatusById(String id) {
    return damagedStatusClient.getById(id);
  }

  @Cacheable(cacheNames = "damagedStatuses")
  public DamagedStatusCollection getDamagedStatusesByName(String name) {
    return damagedStatusClient.getByQuery(NAME + name);
  }

  public DamagedStatus getDamagedStatusByName(String name) {
    var statuses = getDamagedStatusesByName(name);
    if (statuses.getItemDamageStatuses().isEmpty()) {
      throw new BulkEditException("Damaged status not found: " + name);
    }
    return statuses.getItemDamageStatuses().get(0);
  }

  @Cacheable(cacheNames = "noteTypes")
  public NoteType getNoteTypeById(String id) {
    return itemNoteTypeClient.getById(id);
  }

  @Cacheable(cacheNames = "noteTypes")
  public NoteTypeCollection getNoteTypesByName(String name) {
    return itemNoteTypeClient.getByQuery(NAME + name);
  }

  public NoteType getNoteTypeByName(String name) {
    var noteTypes = getNoteTypesByName(name);
    if (noteTypes.getItemNoteTypes().isEmpty()) {
      throw new BulkEditException("Note type not found: " + name);
    }
    return noteTypes.getItemNoteTypes().get(0);
  }

  @Cacheable(cacheNames = "relationships")
  public ElectronicAccessRelationship getRelationshipById(String id) {
    return relationshipClient.getById(id);
  }

  @Cacheable(cacheNames = "relationships")
  public ElectronicAccessRelationshipCollection getRelationshipsByName(String name) {
    return relationshipClient.getByQuery(NAME + name);
  }

  public ElectronicAccessRelationship getElectronicAccessRelationshipByName(String name) {
    var relationships = getRelationshipsByName(name);
    if (relationships.getElectronicAccessRelationships().isEmpty()) {
      throw new BulkEditException("Electronic access relationship not found: " + name);
    }
    return relationships.getElectronicAccessRelationships().get(0);
  }

  @Cacheable(cacheNames = "servicePoints")
  public ServicePoint getServicePointById(String id) {
    return servicePointClient.getById(id);
  }

  @Cacheable(cacheNames = "servicePoints")
  public ServicePoints getServicePointsByName(String name) {
    return servicePointClient.get(NAME + name, 1L);
  }

  public ServicePoint getServicePointByName(String name) {
    var servicePoints = getServicePointsByName(name);
    if (servicePoints.getServicepoints().isEmpty()) {
      throw new BulkEditException("Service point not found: " + name);
    }
    return servicePoints.getServicepoints().get(0);
  }

  @Cacheable(cacheNames = "statisticalCodes")
  public StatisticalCode getStatisticalCodeById(String id) {
    return statisticalCodeClient.getById(id);
  }

  @Cacheable(cacheNames = "statisticalCodes")
  public StatisticalCodeCollection getStatisticalCodesByCode(String code) {
    return statisticalCodeClient.getByQuery(CODE + code);
  }

  public StatisticalCode getStatisticalCodeByName(String name) {
    var codes = getStatisticalCodesByCode(name);
    if (codes.getStatisticalCodes().isEmpty()) {
      throw new BulkEditException("Statistical code not found: " + name);
    }
    return codes.getStatisticalCodes().get(0);
  }

  @Cacheable(cacheNames = "users")
  public User getStaffMemberById(String id) {
    return userClient.getUserById(id);
  }

  @Cacheable(cacheNames = "users")
  public UserCollection getUsersByUsername(String username) {
    return userClient.getUserByQuery(USERNAME + username);
  }

  public User getUserByUserName(String name) {
    var users = getUsersByUsername(name);
    if (users.getUsers().isEmpty()) {
      throw new BulkEditException("User not found: " + name);
    }
    return users.getUsers().get(0);
  }

  @Cacheable(cacheNames = "locations")
  public ItemLocationCollection getItemLocationsByName(String name) {
    return locationClient.getLocationByQuery(NAME + name);
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
    return materialTypeClient.getByQuery(NAME + name);
  }

  public MaterialType getMaterialTypeByName(String name) {
    var types = getMaterialTypesByName(name);
    if (types.getMtypes().isEmpty()) {
      throw new BulkEditException("Material type not found: " + name);
    }
    return types.getMtypes().get(0);
  }

  @Cacheable(cacheNames = "briefHoldings")
  public BriefHoldingsRecordCollection getBriefHoldingsByHrid(String hrid) {
    return holdingClient.getByQuery(HRID + hrid);
  }

  public BriefHoldingsRecord getBriefHoldingsRecordByHrid(String hrid) {
    var holdings = getBriefHoldingsByHrid(hrid);
    if (holdings.getHoldingsRecords().isEmpty()) {
      throw new BulkEditException("Holdings record not found: " + hrid);
    }
    return holdings.getHoldingsRecords().get(0);
  }

  @Cacheable(cacheNames = "briefInstances")
  public BriefInstanceCollection getBriefInstancesByHrid(String hrid) {
    return instanceClient.getByQuery(HRID + hrid);
  }

  public BriefInstance getBriefInstanceByHrid(String hrid) {
    var instances = getBriefInstancesByHrid(hrid);
    if (instances.getInstances().isEmpty()) {
      throw new BulkEditException("Instance not found: " + hrid);
    }
    return instances.getInstances().get(0);
  }

  @Cacheable(cacheNames = "loanTypes")
  public LoanTypeCollection getLoanTypesByName(String name) {
    return loanTypeClient.getByQuery(NAME + name);
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
    var locationJson = locationClient.getLocation(holdingJson.get("effectiveLocationId").asText());
    return locationJson.get("name").asText();
  }

  @Cacheable(cacheNames = "allowedStatuses")
  public List<String> getAllowedStatuses(String statusName) {
    var configurations = configurationClient.getConfigurations(String.format(BULK_EDIT_CONFIGURATIONS_QUERY_TEMPLATE, MODULE_NAME, STATUSES_CONFIG_NAME));
    if (configurations.getConfigs().isEmpty()) {
      throw new ConfigurationException("Statuses configuration was not found");
    }
    try {
      var statuses = new ObjectMapper()
        .readValue(configurations.getConfigs().get(0).getValue(), new TypeReference<HashMap<String, List<String>>>() {});
      return statuses.getOrDefault(statusName, Collections.emptyList());
    } catch (JsonProcessingException e) {
      var msg = String.format("Error reading configuration, reason: %s", e.getMessage());
      log.error(msg);
      throw new ConfigurationException(msg);
    }
  }
}
