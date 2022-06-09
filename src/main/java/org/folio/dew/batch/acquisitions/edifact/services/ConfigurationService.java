package org.folio.dew.batch.acquisitions.edifact.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.folio.dew.client.ConfigurationClient;
import org.folio.dew.domain.dto.ConfigurationCollection;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConfigurationService {
  private final ConfigurationClient configurationClient;
  private final ObjectMapper objectMapper;

  private ConfigurationCollection getLocaleSettings() {
    return configurationClient.getConfigurations("(module==ORG and configName==localeSettings)");
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
}
