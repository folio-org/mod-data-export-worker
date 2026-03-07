package org.folio.dew.batch.acquisitions.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.folio.dew.client.TenantAddressesClient;
import org.folio.dew.domain.dto.acquisitions.edifact.TenantAddress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigurationServiceTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final UUID CONFIG_ID = UUID.fromString("1947e709-8d60-42e2-8dde-7566ae446d24");
  private static final String EXPECTED_ADDRESS = "Address 123";

  @Mock
  private TenantAddressesClient tenantAddressesClient;

  @InjectMocks
  private ConfigurationService configurationService;

  @Test
  void getAddressConfig_nullId_returnsEmpty() {
    assertThat(configurationService.getAddressConfig(null)).isEmpty();
  }

  @Test
  void getAddressConfig_matchingAddress_returnsAddress() {
    when(tenantAddressesClient.getById(CONFIG_ID.toString()))
      .thenReturn(createAddressNode(EXPECTED_ADDRESS));

    assertThat(configurationService.getAddressConfig(CONFIG_ID)).isEqualTo(EXPECTED_ADDRESS);
  }

  @Test
  void getAddressConfig_nullResponse_returnsEmpty() {
    when(tenantAddressesClient.getById(CONFIG_ID.toString())).thenReturn(null);

    assertThat(configurationService.getAddressConfig(CONFIG_ID)).isEmpty();
  }

  @Test
  void getAddressConfig_clientThrowsException_returnsEmpty() {
    when(tenantAddressesClient.getById(CONFIG_ID.toString()))
      .thenThrow(new RuntimeException("Connection error"));

    assertThat(configurationService.getAddressConfig(CONFIG_ID)).isEmpty();
  }

  // -- Helper --

  private static TenantAddress createAddressNode(String address) {
    TenantAddress entry = new TenantAddress();
    entry.setAddress(address);
    return entry;
  }
}
