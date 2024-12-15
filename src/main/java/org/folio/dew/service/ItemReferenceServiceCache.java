package org.folio.dew.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.client.CallNumberTypeClient;
import org.folio.dew.client.DamagedStatusClient;
import org.folio.dew.client.ItemNoteTypeClient;
import org.folio.dew.client.StatisticalCodeClient;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;


@Service
@Log4j2
@RequiredArgsConstructor
public class ItemReferenceServiceCache extends FolioExecutionContextManager {

  private final CallNumberTypeClient callNumberTypeClient;
  private final DamagedStatusClient damagedStatusClient;
  private final ItemNoteTypeClient itemNoteTypeClient;
  private final StatisticalCodeClient statisticalCodeClient;
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

  @Cacheable(cacheNames = "damagedStatusNames")
  public String getDamagedStatusNameById(String damagedStatusId, String tenantId) {
    if (isEmpty(damagedStatusId)) {
      return EMPTY;
    }
    try (var context = new FolioExecutionContextSetter(refreshAndGetFolioExecutionContext(tenantId, folioExecutionContext))) {
      return damagedStatusClient.getById(damagedStatusId).getName();
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

  @Cacheable(cacheNames = "statisticalCodeNames")
  public String getStatisticalCodeById(String statisticalCodeId, String tenantId) {
    if (isEmpty(statisticalCodeId)) {
      return EMPTY;
    }
    try (var context = new FolioExecutionContextSetter(refreshAndGetFolioExecutionContext(tenantId, folioExecutionContext))) {
      return statisticalCodeClient.getById(statisticalCodeId).getCode();
    }
  }
}
