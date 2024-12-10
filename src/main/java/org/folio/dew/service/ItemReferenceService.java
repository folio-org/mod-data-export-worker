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

  private final CallNumberTypeClient callNumberTypeClient;
  private final DamagedStatusClient damagedStatusClient;
  private final ItemNoteTypeClient itemNoteTypeClient;
  private final StatisticalCodeClient statisticalCodeClient;
  private final BulkEditProcessingErrorsService errorsService;
  private final FolioExecutionContext folioExecutionContext;

  @Cacheable(cacheNames = "callNumberTypeNames")
  public String getCallNumberTypeNameById(String callNumberTypeId, String tenantId) {
    if (isEmpty(callNumberTypeId)) {
      return EMPTY;
    }
    try (var context = new FolioExecutionContextSetter(refreshAndGetFolioExecutionContext(tenantId, folioExecutionContext))) {
      return callNumberTypeClient.getById(callNumberTypeId).getName();
    }
  }

  public String getCallNumberTypeNameById(String callNumberTypeId, ErrorServiceArgs args, String tenantId) {
    try {
      return getCallNumberTypeNameById(callNumberTypeId, tenantId);
    } catch (NotFoundException e) {
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(String.format("Call number type was not found by id: [%s]", callNumberTypeId)), args.getFileName());
      return callNumberTypeId;
    }
  }

  @Cacheable(cacheNames = "damagedStatusNames")
  public String getDamagedStatusNameById(String damagedStatusId, String tenantId) {
    if (isEmpty(damagedStatusId)) {
      return EMPTY;
    }
    try (var context = new FolioExecutionContextSetter(refreshAndGetFolioExecutionContext(tenantId, folioExecutionContext))) {
      return damagedStatusClient.getById(damagedStatusId).getName();
    }
  }

  public String getDamagedStatusNameById(String damagedStatusId, ErrorServiceArgs args, String tenantId) {
    try {
      return getDamagedStatusNameById(damagedStatusId, tenantId);
    } catch (NotFoundException e) {
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(String.format("Damaged status was not found by id: [%s]", damagedStatusId)), args.getFileName());
      return damagedStatusId;
    }
  }

  @Cacheable(cacheNames = "noteTypeNames")
  public String getNoteTypeNameById(String noteTypeId, String tenantId) {
    if (isEmpty(noteTypeId)) {
      return EMPTY;
    }
    try (var context = new FolioExecutionContextSetter(refreshAndGetFolioExecutionContext(tenantId, folioExecutionContext))) {
      return itemNoteTypeClient.getById(noteTypeId).getName();
    }
  }

  public String getNoteTypeNameById(String noteTypeId, ErrorServiceArgs args, String tenantId) {
    try {
      return getNoteTypeNameById(noteTypeId, tenantId);
    } catch (NotFoundException e) {
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(String.format("Note type was not found by id: [%s]", noteTypeId)), args.getFileName());
      return noteTypeId;
    }
  }

  @Cacheable(cacheNames = "statisticalCodeNames")
  public String getStatisticalCodeById(String statisticalCodeId, String tenantId) {
    if (isEmpty(statisticalCodeId)) {
      return EMPTY;
    }
    try (var context = new FolioExecutionContextSetter(refreshAndGetFolioExecutionContext(tenantId, folioExecutionContext))) {
      return statisticalCodeClient.getById(statisticalCodeId).getCode();
    }
  }

  public String getStatisticalCodeById(String statisticalCodeId, ErrorServiceArgs args, String tenantId) {
    try {
      return getStatisticalCodeById(statisticalCodeId, tenantId);
    } catch (NotFoundException e) {
      errorsService.saveErrorInCSV(args.getJobId(), args.getIdentifier(), new BulkEditException(String.format("Statistical code was not found by id: [%s]", statisticalCodeId)), args.getFileName());
      return statisticalCodeId;
    }
  }
}
