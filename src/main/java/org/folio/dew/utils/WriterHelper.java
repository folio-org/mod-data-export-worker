package org.folio.dew.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.folio.dew.domain.dto.HoldingsFormat;

@UtilityClass
public class WriterHelper {
  @SneakyThrows
  public static String enrichHoldingsJson(HoldingsFormat item, ObjectMapper objectMapper) {
    var holdingsFormatJson = objectMapper.valueToTree(item);
    var holdingsJson = (ObjectNode) objectMapper.valueToTree(item.getOriginal());
    holdingsJson.putIfAbsent("instanceHrid", holdingsFormatJson.get("instanceHrid"));
    holdingsJson.putIfAbsent("itemBarcode", holdingsFormatJson.get("itemBarcode"));
    holdingsJson.putIfAbsent("instanceTitle", holdingsFormatJson.get("instance"));
    return objectMapper.writeValueAsString(holdingsJson);
  }
}
