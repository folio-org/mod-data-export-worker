package org.folio.dew.batch.acquisitions.edifact.services;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dew.client.ConfigurationClient;
import org.folio.dew.domain.dto.ModelConfiguration;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfigurationService {

  private static final Logger logger = LogManager.getLogger();

  private final ConfigurationClient configurationClient;

  private ModelConfiguration getConfigById(String configId) {
    return configurationClient.getConfigById(configId);
  }

  @Cacheable(cacheNames = "addressConfiguration")
  public String getAddressConfig(UUID shipToConfigId) {
    if (shipToConfigId == null) {
      logger.warn("getAddressConfig:: 'shipTo' field of composite purchase order is null");
      return "";
    }

    var addressConfig = getConfigById(shipToConfigId.toString());
    try {
      var valueJsonObject = new JSONObject(addressConfig.getValue());
      return valueJsonObject.has("address") ? valueJsonObject.get("address").toString() : "";
    } catch (JSONException e) {
      logger.error("getAddressConfig:: Couldn't convert configValue: {} to json", addressConfig);
      return "";
    }
  }
}
