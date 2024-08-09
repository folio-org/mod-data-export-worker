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
    String addressConfigId = "1947e709-8d60-42e2-8dde-7566ae446d24"; // don't change it is hard coded

    String addressFirst = configurationService.getAddressConfig(UUID.fromString(addressConfigId));
    assertEquals("Address 123", addressFirst);
  }
}
