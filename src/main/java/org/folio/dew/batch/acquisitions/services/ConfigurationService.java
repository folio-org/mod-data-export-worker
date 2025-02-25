package org.folio.dew.batch.acquisitions.services;

import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dew.client.ConfigurationClient;
import org.folio.dew.domain.dto.ModelConfiguration;
import org.folio.dew.error.NotFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfigurationService {

  private static final Logger logger = LogManager.getLogger();

  private final ConfigurationClient configurationClient;
  private final ObjectMapper objectMapper;

  @Cacheable(cacheNames = "addressConfiguration")
  public String getAddressConfig(UUID shipToConfigId) {
    if (shipToConfigId == null) {
      logger.warn("getAddressConfig:: 'shipTo' field of composite purchase order is null");
      return "";
    }

    ModelConfiguration addressConfig;
    try {
      addressConfig = getConfigById(shipToConfigId.toString());
    } catch (NotFoundException e) {
      logger.warn("getAddressConfig:: cannot find config by id: '{}'", shipToConfigId);
      return "";
    }
    if (addressConfig.getValue() == null) {
      logger.warn("getAddressConfig:: 'address config with id '{}' is not found", shipToConfigId);
      return "";
    }
    try {
      JsonNode valueJsonObject = objectMapper.readTree(addressConfig.getValue());
      return valueJsonObject.has("address") ? valueJsonObject.get("address").asText() : "";
    } catch (JsonProcessingException e) {
      logger.error("getAddressConfig:: Couldn't convert configValue: {} to json", addressConfig, e);
      return "";
    }
  }

  private ModelConfiguration getConfigById(String configId) {
    try {
      return configurationClient.getConfigById(configId);
    } catch (NotFoundException e) {
      throw new NotFoundException("Configuration entry not found for id: " + configId, e);
    }
  }
}
