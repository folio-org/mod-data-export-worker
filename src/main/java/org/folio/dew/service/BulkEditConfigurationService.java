package org.folio.dew.service;

import static org.folio.dew.domain.dto.InventoryItemStatus.NameEnum.AVAILABLE;
import static org.folio.dew.domain.dto.InventoryItemStatus.NameEnum.INTELLECTUAL_ITEM;
import static org.folio.dew.domain.dto.InventoryItemStatus.NameEnum.IN_PROCESS;
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
import org.folio.dew.domain.dto.InventoryItemStatus;
import org.folio.dew.domain.dto.ModelConfiguration;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@Log4j2
@RequiredArgsConstructor
public class BulkEditConfigurationService {
  private final ConfigurationClient configurationClient;
  private final ObjectMapper objectMapper;

  private static final Map<InventoryItemStatus.NameEnum, List<InventoryItemStatus.NameEnum>> allowedStatuses = new EnumMap<>(InventoryItemStatus.NameEnum.class);

  static {
    allowedStatuses.put(AVAILABLE,
      Arrays.asList(MISSING,
        WITHDRAWN,
        IN_PROCESS,
        IN_PROCESS_NON_REQUESTABLE_,
        INTELLECTUAL_ITEM,
        LONG_MISSING,
        RESTRICTED,
        UNAVAILABLE,
        UNKNOWN));
    allowedStatuses.put(MISSING,
      Arrays.asList(AVAILABLE,
        WITHDRAWN,
        IN_PROCESS_NON_REQUESTABLE_,
        INTELLECTUAL_ITEM,
        LONG_MISSING,
        RESTRICTED,
        UNAVAILABLE,
        UNKNOWN));
    allowedStatuses.put(WITHDRAWN,
      Arrays.asList(AVAILABLE,
        MISSING,
        IN_PROCESS_NON_REQUESTABLE_,
        INTELLECTUAL_ITEM,
        LONG_MISSING,
        RESTRICTED,
        UNAVAILABLE,
        UNKNOWN));
    allowedStatuses.put(IN_PROCESS_NON_REQUESTABLE_,
      Arrays.asList(AVAILABLE,
        MISSING,
        WITHDRAWN,
        INTELLECTUAL_ITEM,
        LONG_MISSING,
        RESTRICTED,
        UNAVAILABLE,
        UNKNOWN));
    allowedStatuses.put(INTELLECTUAL_ITEM,
      Arrays.asList(AVAILABLE,
        MISSING,
        WITHDRAWN,
        IN_PROCESS_NON_REQUESTABLE_,
        LONG_MISSING,
        RESTRICTED,
        UNAVAILABLE,
        UNKNOWN));
    allowedStatuses.put(LONG_MISSING,
      Arrays.asList(AVAILABLE,
        MISSING,
        WITHDRAWN,
        IN_PROCESS_NON_REQUESTABLE_,
        INTELLECTUAL_ITEM,
        RESTRICTED,
        UNAVAILABLE,
        UNKNOWN));
    allowedStatuses.put(RESTRICTED,
      Arrays.asList(AVAILABLE,
        MISSING,
        WITHDRAWN,
        IN_PROCESS_NON_REQUESTABLE_,
        INTELLECTUAL_ITEM,
        LONG_MISSING,
        UNAVAILABLE,
        UNKNOWN));
    allowedStatuses.put(UNAVAILABLE,
      Arrays.asList(AVAILABLE,
        MISSING,
        WITHDRAWN,
        IN_PROCESS_NON_REQUESTABLE_,
        INTELLECTUAL_ITEM,
        LONG_MISSING,
        RESTRICTED,
        UNKNOWN));
    allowedStatuses.put(UNKNOWN,
      Arrays.asList(AVAILABLE,
        MISSING,
        WITHDRAWN,
        IN_PROCESS_NON_REQUESTABLE_,
        INTELLECTUAL_ITEM,
        LONG_MISSING,
        RESTRICTED,
        UNAVAILABLE));
    allowedStatuses.put(IN_PROCESS,
      Arrays.asList(MISSING,
        WITHDRAWN));
  }

  public void updateBulkEditConfiguration() {
    var configurations = configurationClient.getConfigurations(String.format(BULK_EDIT_CONFIGURATIONS_QUERY_TEMPLATE, MODULE_NAME, STATUSES_CONFIG_NAME));
    if (!configurations.getConfigs().isEmpty()) {
      log.info("Deleting old bulk edit statuses configuration");
      configurationClient.deleteConfiguration(configurations.getConfigs().get(0).getId());
    }
    configurationClient.postConfiguration(buildDefaultConfig());
  }

  @SneakyThrows
  private ModelConfiguration buildDefaultConfig() {
   return new ModelConfiguration()
      .module(MODULE_NAME)
      .configName(STATUSES_CONFIG_NAME)
      ._default(true)
      .enabled(true)
      .value(objectMapper.writeValueAsString(allowedStatuses));
  }

  public static Map<InventoryItemStatus.NameEnum, List<InventoryItemStatus.NameEnum>> getAllowedStatuses() {
    return allowedStatuses;
  }
}
