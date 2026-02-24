package org.folio.dew.batch.acquisitions.services;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dew.client.TenantAddressesClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfigurationService {

  private static final Logger logger = LogManager.getLogger();
  private static final String ADDRESSES = "addresses";
  private static final String ID = "id";
  private static final String ADDRESS = "address";

  private final TenantAddressesClient tenantAddressesClient;

  @Cacheable(cacheNames = "addressConfiguration")
  public String getAddressConfig(UUID shipToConfigId) {
    if (shipToConfigId == null) {
      logger.warn("getAddressConfig:: shipToConfigId is null");
      return "";
    }
    try {
      JsonNode addressesResponse = tenantAddressesClient.getTenantAddresses();

      if (addressesResponse == null || !addressesResponse.has(ADDRESSES)) {
        logger.warn("getAddressConfig:: No addresses found in tenant-addresses response");
        return "";
      }

      JsonNode addressesList = addressesResponse.get(ADDRESSES);
      if (!addressesList.isArray() || addressesList.isEmpty()) {
        logger.warn("getAddressConfig:: Addresses list is empty");
        return "";
      }

      for (JsonNode addressEntry : addressesList) {
        String addressId = addressEntry.path(ID).asText(null);
        if (StringUtils.equals(shipToConfigId.toString(), addressId)) {
          logger.info("getAddressConfig:: Found address with id '{}'", shipToConfigId);
          return addressEntry.path(ADDRESS).asText("");
        }
      }

      return "";
    } catch (Exception e) {
      logger.warn("getAddressConfig:: Cannot find address by id: '{}'", shipToConfigId, e);
      return "";
    }
  }
}
