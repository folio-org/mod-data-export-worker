package org.folio.dew.batch.acquisitions.edifact;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.folio.dew.BaseBatchTest;
import org.folio.dew.batch.acquisitions.edifact.services.ConfigurationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ConfigurationServiceTest extends BaseBatchTest {

  @Autowired
  private ConfigurationService configurationService;

  @Test
  void getAddress() {
    String addressConfigId = "1947e709-8d60-42e2-8dde-7566ae446d24"; // don't change, it is hardcoded in configurations.json

    String address = configurationService.getAddressConfig(UUID.fromString(addressConfigId));
    assertEquals("Address 123", address);
  }

  @Test
  void getAddressConfigByNullId() {
    String address = configurationService.getAddressConfig(null);

    assertEquals("", address);
  }

  @Test
  void getEmptyAddressConfig() {
    String addressConfigId = "116a38c2-cac3-4f08-816b-afebfebe453d"; // non-existing config

    String address = configurationService.getAddressConfig(UUID.fromString(addressConfigId));

    assertEquals("", address);
  }

  @Test
  void getAddressConfigWithoutAddressValue() {
    String addressConfigId = "8ea92aa2-7b11-4f0e-9ed2-ab8fe281f37f"; // non-existing config

    String address = configurationService.getAddressConfig(UUID.fromString(addressConfigId));

    assertEquals("", address);
  }
}
