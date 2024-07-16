package org.folio.dew.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.folio.dew.domain.dto.ExtendedHoldingsRecord;
import org.folio.dew.domain.dto.ExtendedInstance;
import org.folio.dew.domain.dto.ExtendedItem;
import org.folio.dew.domain.dto.HoldingsFormat;
import org.folio.dew.domain.dto.InstanceFormat;
import org.folio.dew.domain.dto.ItemFormat;

@UtilityClass
public class WriterHelper {

  public static final String ENTITY = "entity";

  @SneakyThrows
  public static String enrichItemsJson(ItemFormat item, ObjectMapper objectMapper) {

    var extendedItemObject = new ExtendedItem().entity(item.getOriginal()).tenantId(item.getTenantId());
    var extendedItemJson = (ObjectNode) objectMapper.valueToTree(extendedItemObject);
    var itemFormatJson = objectMapper.valueToTree(item);
    ((ObjectNode) extendedItemJson.get(ENTITY)).putIfAbsent("holdingsData", itemFormatJson.get("holdingsData"));
    return objectMapper.writeValueAsString(extendedItemJson);
  }

  @SneakyThrows
  public static String enrichHoldingsJson(HoldingsFormat item, ObjectMapper objectMapper) {
    var extendedHoldingsRecordObject = new ExtendedHoldingsRecord().entity(item.getOriginal()).tenantId(item.getTenantId());
    var extendedHoldingsRecordJson = (ObjectNode) objectMapper.valueToTree(extendedHoldingsRecordObject);
    var holdingsFormatJson = objectMapper.valueToTree(item);
    ((ObjectNode) extendedHoldingsRecordJson.get(ENTITY)).putIfAbsent("instanceHrid", holdingsFormatJson.get("instanceHrid"));
    ((ObjectNode) extendedHoldingsRecordJson.get(ENTITY)).putIfAbsent("itemBarcode", holdingsFormatJson.get("itemBarcode"));
    ((ObjectNode) extendedHoldingsRecordJson.get(ENTITY)).putIfAbsent("instanceTitle", holdingsFormatJson.get("instance"));
    return objectMapper.writeValueAsString(extendedHoldingsRecordJson);
  }
  @SneakyThrows
  public static String enrichInstancesJson(InstanceFormat item, ObjectMapper objectMapper) {
    var extendedInstanceObject = new ExtendedInstance().entity(item.getOriginal()).tenantId(item.getTenantId());
    var extendedInstanceJson = (ObjectNode) objectMapper.valueToTree(extendedInstanceObject);

    var instancesFormatJson = objectMapper.valueToTree(item);
    ((ObjectNode) extendedInstanceJson.get(ENTITY)).putIfAbsent("ISSN", instancesFormatJson.get("issn"));
    ((ObjectNode) extendedInstanceJson.get(ENTITY)).putIfAbsent("ISBN", instancesFormatJson.get("isbn"));

    return objectMapper.writeValueAsString(extendedInstanceJson);
  }
}
