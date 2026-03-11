package org.folio.dew.batch.acquisitions.services;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dew.client.TenantAddressesClient;
import org.folio.dew.domain.dto.acquisitions.edifact.TenantAddress;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import static java.util.Optional.ofNullable;

@Service
@RequiredArgsConstructor
public class ConfigurationService {

  private static final Logger logger = LogManager.getLogger();
  private static final String ADDRESS = "address";

  private final TenantAddressesClient tenantAddressesClient;

  @Cacheable(cacheNames = "addressConfiguration")
  public String getAddressConfig(UUID shipToConfigId) {
    if (shipToConfigId == null) {
      logger.warn("getAddressConfig:: shipToConfigId is null");
      return "";
    }
    try {
      TenantAddress addressResponse = tenantAddressesClient.getById(shipToConfigId.toString());

      if (addressResponse == null) {
        logger.warn("getAddressConfig:: No address found for id '{}'", shipToConfigId);
        return "";
      }

      logger.info("getAddressConfig:: Found address with id '{}'", shipToConfigId);
      return ofNullable(addressResponse.getAddress()).orElse("");
    } catch (Exception e) {
      logger.warn("getAddressConfig:: Cannot find address by id: '{}'", shipToConfigId, e);
      return "";
    }
  }
}
