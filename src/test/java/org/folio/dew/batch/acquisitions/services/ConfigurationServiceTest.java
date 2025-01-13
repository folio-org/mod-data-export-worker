package org.folio.dew.batch.acquisitions.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import java.util.stream.Stream;

import org.folio.dew.BaseBatchTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

class ConfigurationServiceTest extends BaseBatchTest {

  @Autowired
  private ConfigurationService configurationService;

  static Stream<Arguments> provideAddressConfigData() {
    return Stream.of(
      Arguments.of("1947e709-8d60-42e2-8dde-7566ae446d24", "Address 123"), // existing config
      Arguments.of(null, ""), // null config ID
      Arguments.of("116a38c2-cac3-4f08-816b-afebfebe453d", ""), // non-existing config
      Arguments.of("8ea92aa2-7b11-4f0e-9ed2-ab8fe281f37f", "") // config without address value
    );
  }

  @ParameterizedTest
  @MethodSource("provideAddressConfigData")
  void testGetAddressConfig(String addressConfigId, String expectedAddress) {
    UUID configId = addressConfigId != null ? UUID.fromString(addressConfigId) : null;
    String address = configurationService.getAddressConfig(configId);
    assertEquals(expectedAddress, address);
  }
}
