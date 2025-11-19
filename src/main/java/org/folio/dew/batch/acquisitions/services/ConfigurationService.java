package org.folio.dew.batch.acquisitions.services;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dew.client.SettingsClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfigurationService {

  private static final Logger logger = LogManager.getLogger();

  private final SettingsClient settingsClient;
  private final ObjectMapper objectMapper;

  @Cacheable(cacheNames = "addressConfiguration")
  public String getAddressConfig(UUID shipToConfigId) {
    if (shipToConfigId == null) {
      logger.warn("getAddressConfig:: shipToConfigId is null");
      return "";
    }
    try {
      var settingEntry = settingsClient.getSettingById(shipToConfigId.toString());

      if (settingEntry == null || !settingEntry.containsKey("value")) {
        logger.warn("getAddressConfig:: Address on the config with id '{}' is not found", shipToConfigId);
        return "";
      }

      var value = settingEntry.get("value");
      JsonNode valueJsonObject = objectMapper.valueToTree(value);
      return valueJsonObject.has("address") ? valueJsonObject.get("address").asText() : "";
    } catch (Exception e) {
      logger.warn("getAddressConfig:: Cannot find config by id: '{}'", shipToConfigId, e);
      return "";
    }
  }
}
