package org.folio.dew.service;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.dew.utils.Constants.BULK_EDIT_CONFIGURATIONS_QUERY_TEMPLATE;
import static org.folio.dew.utils.Constants.MODULE_NAME;
import static org.folio.dew.utils.Constants.STATUSES_CONFIG_NAME;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
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
import org.folio.dew.domain.dto.ItemLocationCollection;
import org.folio.dew.domain.dto.LoanTypeCollection;
import org.folio.dew.domain.dto.MaterialTypeCollection;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.error.ConfigurationException;
import org.folio.dew.error.NotFoundException;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@Service
@Log4j2
@RequiredArgsConstructor
public class ItemReferenceService extends FolioExecutionContextManager {
  private static final String QUERY_PATTERN_NAME = "name==\"%s\"";
  public static final String EFFECTIVE_LOCATION_ID = "effectiveLocationId";

  private final CallNumberTypeClient callNumberTypeClient;
  private final DamagedStatusClient damagedStatusClient;
  private final ItemNoteTypeClient itemNoteTypeClient;
  private final StatisticalCodeClient statisticalCodeClient;
  private final LocationClient locationClient;
  private final MaterialTypeClient materialTypeClient;
  private final HoldingClient holdingClient;
  private final LoanTypeClient loanTypeClient;
  private final ConfigurationClient configurationClient;
  private final ObjectMapper objectMapper;
  private final BulkEditProcessingErrorsService errorsService;
  private final FolioExecutionContext folioExecutionContext;

  @Cacheable(cacheNames = "callNumberTypeNames")
  public String getCallNumberTypeNameById(String callNumberTypeId, ErrorServiceArgs args, String tenantId) {
    try (var context = new FolioExecutionContextSetter(refreshAndGetFolioExecutionContext(tenantId, folioExecutionContext))) {
      return isEmpty(callNumberTypeId) ? EMPTY : callNumberTypeClient.getById(callNumberTypeId).getName();
    } catch (NotFoundException e) {
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(String.format("Call number type was not found by id: [%s]", callNumberTypeId)), args.getFileName());
      return callNumberTypeId;
    }
  }

  @Cacheable(cacheNames = "damagedStatusNames")
  public String getDamagedStatusNameById(String damagedStatusId, ErrorServiceArgs args, String tenantId) {
    try (var context = new FolioExecutionContextSetter(refreshAndGetFolioExecutionContext(tenantId, folioExecutionContext))) {
      return isEmpty(damagedStatusId) ? EMPTY : damagedStatusClient.getById(damagedStatusId).getName();
    } catch (NotFoundException e) {
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(String.format("Damaged status was not found by id: [%s]", damagedStatusId)), args.getFileName());
      return damagedStatusId;
    }
  }

  @Cacheable(cacheNames = "noteTypeNames")
  public String getNoteTypeNameById(String noteTypeId, ErrorServiceArgs args, String tenantId) {
    try (var context = new FolioExecutionContextSetter(refreshAndGetFolioExecutionContext(tenantId, folioExecutionContext))) {
      return isEmpty(noteTypeId) ? EMPTY : itemNoteTypeClient.getById(noteTypeId).getName();
    } catch (NotFoundException e) {
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(String.format("Note type was not found by id: [%s]", noteTypeId)), args.getFileName());
      return noteTypeId;
    }
  }

  @Cacheable(cacheNames = "statisticalCodeNames")
  public String getStatisticalCodeById(String statisticalCodeId, ErrorServiceArgs args, String tenantId) {
    try (var context = new FolioExecutionContextSetter(refreshAndGetFolioExecutionContext(tenantId, folioExecutionContext))) {
      return isEmpty(statisticalCodeId) ? EMPTY : statisticalCodeClient.getById(statisticalCodeId).getCode();
    } catch (NotFoundException e) {
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(String.format("Statistical code was not found by id: [%s]", statisticalCodeId)), args.getFileName());
      return statisticalCodeId;
    }
  }

  @Cacheable(cacheNames = "locations")
  public ItemLocationCollection getItemLocationsByName(String name) {
    return locationClient.getLocationByQuery(String.format(QUERY_PATTERN_NAME, name));
  }

  @Cacheable(cacheNames = "materialTypes")
  public MaterialTypeCollection getMaterialTypesByName(String name) {
    return materialTypeClient.getByQuery(String.format(QUERY_PATTERN_NAME, name));
  }

  @Cacheable(cacheNames = "loanTypes")
  public LoanTypeCollection getLoanTypesByName(String name) {
    return loanTypeClient.getByQuery(String.format(QUERY_PATTERN_NAME, name));
  }

  @Cacheable(cacheNames = "holdings")
  public String getHoldingEffectiveLocationCodeById(String id) {
    var holdingJson = holdingClient.getHoldingById(id);
    var effectiveLocationId = isEmpty(holdingJson.get(EFFECTIVE_LOCATION_ID)) ? getHoldingsEffectiveLocation(holdingJson) : holdingJson.get(EFFECTIVE_LOCATION_ID);
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
}
