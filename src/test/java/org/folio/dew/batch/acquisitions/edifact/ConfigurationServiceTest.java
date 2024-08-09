package org.folio.dew.batch.acquisitions.edifact;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.folio.dew.BaseBatchTest;
import org.folio.dew.batch.acquisitions.edifact.services.ConfigurationService;
import org.folio.dew.client.ConfigurationClient;
import org.folio.dew.domain.dto.ModelConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@ExtendWith(MockitoExtension.class)
class ConfigurationServiceTest extends BaseBatchTest {

  @Autowired
  private ConfigurationService configurationService;
  @MockBean
  private ConfigurationClient configurationClient;

  @Test
  void getAddress() {
    String addressConfigId = UUID.randomUUID().toString();
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
