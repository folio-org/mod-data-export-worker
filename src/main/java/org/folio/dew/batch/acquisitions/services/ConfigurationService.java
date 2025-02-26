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
      logger.warn("getAddressConfig:: shipToConfigId is null");
      return "";
    }
    ModelConfiguration config;
    try {
      config = configurationClient.getConfigById(shipToConfigId.toString());
    } catch (NotFoundException e) {
      logger.warn("getAddressConfig:: Cannot find config by id: '{}'", shipToConfigId);
      return "";
    }
    if (config.getValue() == null) {
      logger.warn("getAddressConfig:: Address on the config with id '{}' is not found", shipToConfigId);
      return "";
    }
    try {
      JsonNode valueJsonObject = objectMapper.readTree(config.getValue());
      return valueJsonObject.has("address") ? valueJsonObject.get("address").asText() : "";
    } catch (JsonProcessingException e) {
      logger.error("getAddressConfig:: Cannot convert config value: {} to json", config, e);
      return "";
    }
  }
}
