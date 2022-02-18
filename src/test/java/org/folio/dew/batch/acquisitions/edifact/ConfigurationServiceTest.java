package org.folio.dew.batch.acquisitions.edifact;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.folio.dew.BaseBatchTest;
import org.folio.dew.batch.acquisitions.edifact.services.ConfigurationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;


class ConfigurationServiceTest extends BaseBatchTest {
  @Autowired
  private ConfigurationService configurationService;

  @Test
  void getCurrency() {
    String currency = configurationService.getSystemCurrency();
    assertEquals("USD", currency);
  }
}
