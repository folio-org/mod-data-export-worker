package org.folio.dew.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.folio.dew.domain.dto.HoldingsFormat;
import org.folio.dew.domain.dto.InstanceFormat;
import org.folio.dew.domain.dto.ItemFormat;

@UtilityClass
public class WriterHelper {
  @SneakyThrows
  public static String enrichItemsJson(ItemFormat item, ObjectMapper objectMapper) {
    var itemFormatJson = objectMapper.valueToTree(item);
    var itemJson = (ObjectNode) objectMapper.valueToTree(item.getOriginal());
    itemJson.putIfAbsent("holdingsData", itemFormatJson.get("holdingsData"));
    return objectMapper.writeValueAsString(itemJson);
  }

  @SneakyThrows
  public static String enrichHoldingsJson(HoldingsFormat item, ObjectMapper objectMapper) {
    var holdingsFormatJson = objectMapper.valueToTree(item);
    var holdingsJson = (ObjectNode) objectMapper.valueToTree(item.getOriginal());
    holdingsJson.putIfAbsent("instanceHrid", holdingsFormatJson.get("instanceHrid"));
    holdingsJson.putIfAbsent("itemBarcode", holdingsFormatJson.get("itemBarcode"));
    holdingsJson.putIfAbsent("instanceTitle", holdingsFormatJson.get("instanceTitle"));
    return objectMapper.writeValueAsString(holdingsJson);
  }
  @SneakyThrows
  public static String enrichInstancesJson(InstanceFormat item, ObjectMapper objectMapper) {
    var instancesFormatJson = objectMapper.valueToTree(item);
    var instancesJson = (ObjectNode) objectMapper.valueToTree(item.getOriginal());
    instancesJson.putIfAbsent("ISSN", instancesFormatJson.get("issn"));
    instancesJson.putIfAbsent("ISBN", instancesFormatJson.get("isbn"));
    return objectMapper.writeValueAsString(instancesJson);
  }
}
