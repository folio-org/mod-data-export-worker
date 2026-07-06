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
 * Thin cached wrapper over {@link CustomFieldsClient} that returns the custom-field definitions
 * for an entity type, indexed by refId. The definitions live on {@code mod-orders-storage}, so the
 * {@code custom-fields} ({@code interfaceType: multiple}) call is targeted with an explicit module id.
 *
 * <p>Degrades gracefully: if the interface is unavailable (e.g. 404 at the gateway) the export still
 * goes out, just without resolved custom-field tokens.
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class CustomFieldDefinitionService {

  private static final int LIMIT = 1000;
  private static final String MODULE_ID = "mod-orders-storage";

  private final CustomFieldsClient customFieldsClient;

  @Cacheable(cacheNames = "customFieldDefinitions", key = "#entityType")
  public Map<String, CustomField> getDefinitionsByRefId(String entityType) {
    Map<String, CustomField> byRefId = new LinkedHashMap<>();
    try {
      var collection = customFieldsClient.getCustomFields("entityType==" + entityType, LIMIT, MODULE_ID);
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