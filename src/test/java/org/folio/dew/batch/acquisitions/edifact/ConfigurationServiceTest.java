package org.folio.dew.batch.acquisitions.edifact;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.folio.dew.BaseBatchTest;
import org.folio.dew.batch.acquisitions.edifact.services.ConfigurationService;
import org.folio.dew.client.ConfigurationClient;
import org.folio.dew.domain.dto.ModelConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;


class ConfigurationServiceTest extends BaseBatchTest {
  private static final String addressConfigId = "db5bb2a2-8909-4dce-8b10-59fda5231459";
  @Autowired
  private ConfigurationService configurationService;
  @MockBean
  private ConfigurationClient configurationClient;

  @Test
  void getAddress() {
    var addressConfiguration = new ModelConfiguration()
      .id(addressConfigId)
      .configName("tenant.addresses")
      .value("{\"name\":\"Name 1\",\"address\":\"Address 123\"}");
    Mockito.when(configurationClient.getConfigById(addressConfigId)).thenReturn(addressConfiguration);

    String addressFirst = configurationService.getAddressConfig(UUID.fromString(addressConfigId));
    assertEquals("Address 123", addressFirst);
    // fetching from cache
    String addressSecond = configurationService.getAddressConfig(UUID.fromString(addressConfigId));
    assertEquals("Address 123", addressSecond);

    // configurationClient is invoked only one time
    Mockito.verify(configurationClient, Mockito.times(1)).getConfigById(addressConfigId);
  }
}
