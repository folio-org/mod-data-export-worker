package org.folio.dew.service;

import static org.folio.dew.domain.dto.InventoryItemStatus.NameEnum.AVAILABLE;
import static org.folio.dew.domain.dto.InventoryItemStatus.NameEnum.INTELLECTUAL_ITEM;
import static org.folio.dew.domain.dto.InventoryItemStatus.NameEnum.IN_PROCESS_NON_REQUESTABLE_;
import static org.folio.dew.domain.dto.InventoryItemStatus.NameEnum.LONG_MISSING;
import static org.folio.dew.domain.dto.InventoryItemStatus.NameEnum.MISSING;
import static org.folio.dew.domain.dto.InventoryItemStatus.NameEnum.RESTRICTED;
import static org.folio.dew.domain.dto.InventoryItemStatus.NameEnum.UNAVAILABLE;
import static org.folio.dew.domain.dto.InventoryItemStatus.NameEnum.UNKNOWN;
import static org.folio.dew.domain.dto.InventoryItemStatus.NameEnum.WITHDRAWN;
import static org.folio.dew.utils.Constants.BULK_EDIT_CONFIGURATIONS_QUERY_TEMPLATE;
import static org.folio.dew.utils.Constants.STATUSES_CONFIG_NAME;
import static org.folio.dew.utils.Constants.MODULE_NAME;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.client.ConfigurationClient;
import org.folio.dew.domain.dto.ModelConfiguration;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Log4j2
@RequiredArgsConstructor
public class BulkEditConfigurationService {
  private final ConfigurationClient configurationClient;

  private static final Map<String, List<String>> allowedStatuses = new HashMap<>();

  static {
    allowedStatuses.put(AVAILABLE.getValue(),
      Arrays.asList(MISSING.getValue(),
        WITHDRAWN.getValue(),
        IN_PROCESS_NON_REQUESTABLE_.getValue(),
        INTELLECTUAL_ITEM.getValue(),
        LONG_MISSING.getValue(),
        RESTRICTED.getValue(),
        UNAVAILABLE.getValue(),
        UNKNOWN.getValue()));
    allowedStatuses.put(MISSING.getValue(),
      Arrays.asList(AVAILABLE.getValue(),
        WITHDRAWN.getValue(),
        IN_PROCESS_NON_REQUESTABLE_.getValue(),
        INTELLECTUAL_ITEM.getValue(),
        LONG_MISSING.getValue(),
        RESTRICTED.getValue(),
        UNAVAILABLE.getValue(),
        UNKNOWN.getValue()));
    allowedStatuses.put(WITHDRAWN.getValue(),
      Arrays.asList(AVAILABLE.getValue(),
        MISSING.getValue(),
        IN_PROCESS_NON_REQUESTABLE_.getValue(),
        INTELLECTUAL_ITEM.getValue(),
        LONG_MISSING.getValue(),
        RESTRICTED.getValue(),
        UNAVAILABLE.getValue(),
        UNKNOWN.getValue()));
    allowedStatuses.put(IN_PROCESS_NON_REQUESTABLE_.getValue(),
      Arrays.asList(AVAILABLE.getValue(),
        MISSING.getValue(),
        WITHDRAWN.getValue(),
        INTELLECTUAL_ITEM.getValue(),
        LONG_MISSING.getValue(),
        RESTRICTED.getValue(),
        UNAVAILABLE.getValue(),
        UNKNOWN.getValue()));
    allowedStatuses.put(INTELLECTUAL_ITEM.getValue(),
      Arrays.asList(AVAILABLE.getValue(),
        MISSING.getValue(),
        WITHDRAWN.getValue(),
        IN_PROCESS_NON_REQUESTABLE_.getValue(),
        LONG_MISSING.getValue(),
        RESTRICTED.getValue(),
        UNAVAILABLE.getValue(),
        UNKNOWN.getValue()));
    allowedStatuses.put(LONG_MISSING.getValue(),
      Arrays.asList(AVAILABLE.getValue(),
        MISSING.getValue(),
        WITHDRAWN.getValue(),
        IN_PROCESS_NON_REQUESTABLE_.getValue(),
        INTELLECTUAL_ITEM.getValue(),
        RESTRICTED.getValue(),
        UNAVAILABLE.getValue(),
        UNKNOWN.getValue()));
    allowedStatuses.put(RESTRICTED.getValue(),
      Arrays.asList(AVAILABLE.getValue(),
        MISSING.getValue(),
        WITHDRAWN.getValue(),
        IN_PROCESS_NON_REQUESTABLE_.getValue(),
        INTELLECTUAL_ITEM.getValue(),
        LONG_MISSING.getValue(),
        UNAVAILABLE.getValue(),
        UNKNOWN.getValue()));
    allowedStatuses.put(UNAVAILABLE.getValue(),
      Arrays.asList(AVAILABLE.getValue(),
        MISSING.getValue(),
        WITHDRAWN.getValue(),
        IN_PROCESS_NON_REQUESTABLE_.getValue(),
        INTELLECTUAL_ITEM.getValue(),
        LONG_MISSING.getValue(),
        RESTRICTED.getValue(),
        UNKNOWN.getValue()));
    allowedStatuses.put(UNKNOWN.getValue(),
      Arrays.asList(AVAILABLE.getValue(),
        MISSING.getValue(),
        WITHDRAWN.getValue(),
        IN_PROCESS_NON_REQUESTABLE_.getValue(),
        INTELLECTUAL_ITEM.getValue(),
        LONG_MISSING.getValue(),
        RESTRICTED.getValue(),
        UNAVAILABLE.getValue()));
  }

  public void checkBulkEditConfiguration() {
    var configurations = configurationClient.getConfigurations(String.format(BULK_EDIT_CONFIGURATIONS_QUERY_TEMPLATE, MODULE_NAME, STATUSES_CONFIG_NAME));
    if (configurations.getConfigs().isEmpty()) {
      log.info("Bulk-edit configuration was not found, uploading default");
      configurationClient.postConfiguration(buildDefaultConfig());
    }
  }

  @SneakyThrows
  public static ModelConfiguration buildDefaultConfig() {
   return new ModelConfiguration()
      .module(MODULE_NAME)
      .configName(STATUSES_CONFIG_NAME)
      ._default(true)
      .enabled(true)
      .value(new ObjectMapper().writeValueAsString(allowedStatuses));
  }

  public static Map<String, List<String>> getAllowedStatuses() {
    return allowedStatuses;
  }
}
