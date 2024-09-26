package org.folio.dew.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.folio.dew.BaseBatchTest;
import org.folio.dew.client.ConfigurationClient;
import org.folio.dew.domain.dto.ConfigurationCollection;
import org.folio.dew.domain.dto.ModelConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Collections;
import java.util.UUID;

class BulkEditConfigurationServiceTest extends BaseBatchTest {
  @MockBean
  private ConfigurationClient configurationClient;

  @Autowired
  private BulkEditConfigurationService service;

  @Test
  void shouldUpdateConfiguration() {
    var configurationId = UUID.randomUUID().toString();
    when(configurationClient.getConfigurations(anyString()))
      .thenReturn(new ConfigurationCollection()
        .configs(Collections.singletonList(new ModelConfiguration().id(configurationId))));

    service.updateBulkEditConfiguration();

    verify(configurationClient).deleteConfiguration(configurationId);
    verify(configurationClient).postConfiguration(any(ModelConfiguration.class));
  }
}
