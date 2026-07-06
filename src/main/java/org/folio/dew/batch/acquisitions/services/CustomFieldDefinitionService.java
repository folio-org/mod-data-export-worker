package org.folio.dew.batch.acquisitions.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.client.CustomFieldsClient;
import org.folio.dew.domain.dto.acquisitions.customfields.CustomField;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Thin cached wrapper over {@link CustomFieldsClient} that returns the custom-field definitions for
 * an entity type, indexed by refId. Definitions live on {@code mod-orders-storage}, exposed through
 * the {@code interfaceType: multiple} {@code custom-fields} interface, so the call must carry the
 * target module id in {@code X-Okapi-Module-Id}. That id is resolved per tenant by
 * {@link OrdersStorageModuleIdResolver}.
 *
 * <p>Degrades gracefully: if the module id can't be resolved, or the interface is unavailable
 * (e.g. 404/403 at the gateway), an empty map is returned so the export still goes out — just
 * without resolved custom-field tokens.
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class CustomFieldDefinitionService {

  private static final int LIMIT = 1000;

  private final CustomFieldsClient customFieldsClient;
  private final OrdersStorageModuleIdResolver ordersStorageModuleIdResolver;

  @Cacheable(cacheNames = "customFieldDefinitions", key = "@folioExecutionContext.tenantId + ':' + #entityType")
  public Map<String, CustomField> getDefinitionsByRefId(String entityType) {
    Map<String, CustomField> byRefId = new LinkedHashMap<>();
    var moduleId = ordersStorageModuleIdResolver.resolve();
    if (StringUtils.isBlank(moduleId)) {
      log.warn("getDefinitionsByRefId:: Could not resolve mod-orders-storage module id "
        + "- email will be sent without resolved custom-field tokens");
      return byRefId;
    }
    try {
      var collection = customFieldsClient.getCustomFields("entityType==" + entityType, LIMIT, moduleId);
      var definitions = Optional.ofNullable(collection.getCustomFields()).orElseGet(List::of);
      for (CustomField definition : definitions) {
        if (StringUtils.isNotBlank(definition.getRefId())) {
          byRefId.put(definition.getRefId(), definition);
        }
      }
    } catch (RestClientException e) {
      log.warn("getDefinitionsByRefId:: Cannot resolve custom-field definitions for entityType '{}' "
        + "- email will be sent without resolved custom-field tokens", entityType, e);
    }
    return byRefId;
  }
}