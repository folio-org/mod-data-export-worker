package org.folio.dew.batch.acquisitions.edifact.services;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dew.client.ConfigurationClient;
import org.folio.dew.domain.dto.ConfigurationCollection;
import org.folio.dew.domain.dto.ModelConfiguration;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConfigurationService {
  private static final Logger logger = LogManager.getLogger();

  private final ConfigurationClient configurationClient;
  private final ObjectMapper objectMapper;

  private ConfigurationCollection getLocaleSettings() {
    return configurationClient.getConfigurations("(module==ORG and configName==localeSettings)");
  }

  private ModelConfiguration getConfigById(String configId) {
    return configurationClient.getConfigById(configId);
  }

  public String getSystemCurrency() {
    ConfigurationCollection configs = getLocaleSettings();

    if (configs == null || configs.getTotalRecords() == 0) {
      return "USD";
    }

    var jsonObject = objectMapper.valueToTree(configs.getConfigs().get(0).getValue());

    if (!jsonObject.has("currency")) {
      return "USD";
    }

    return String.valueOf(jsonObject.get("currency"));
  }

  public String getAddressConfig(UUID configId) {
    if (configId == null) {
      return "";
    }

    var addressConfig = getConfigById(configId.toString());
    var configValue = addressConfig.getValue();
    try {
      JSONObject jsonObject = new JSONObject(configValue);
      return jsonObject.optString("address", "");
    } catch (JSONException e) {
      logger.error("getAddressConfig:: Couldn't convert configValue: {} to json", configValue);
      return "";
    }
  }
}
