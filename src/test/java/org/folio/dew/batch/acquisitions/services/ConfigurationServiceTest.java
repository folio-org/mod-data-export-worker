package org.folio.dew.batch.acquisitions.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import java.util.stream.Stream;
import org.folio.dew.BaseBatchTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

class ConfigurationServiceTest extends BaseBatchTest {

  @Autowired
  private ConfigurationService configurationService;

  @BeforeAll
  static void beforeAll() {
    setUpTenant("diku");
  }

  static Stream<Arguments> testGetAddressConfigArgs() {
    return Stream.of(
      Arguments.of(null, ""), // No config id
      Arguments.of("1947e709-8d60-42e2-8dde-7566ae446d24", "Address 123"), // Config with address
      Arguments.of("8ea92aa2-7b11-4f0e-9ed2-ab8fe281f37f", ""), // Config without address
      Arguments.of("116a38c2-cac3-4f08-816b-afebfebe453d", ""), // Config without a body
      Arguments.of("c5cefe49-e4d4-433e-b286-24ffd935b043", "")  // No config
    );
  }

  @ParameterizedTest
  @MethodSource("testGetAddressConfigArgs")
  void testGetAddressConfig(String addressConfigId, String expectedAddress) {
    var configId = addressConfigId != null ? UUID.fromString(addressConfigId) : null;
    assertEquals(expectedAddress, configurationService.getAddressConfig(configId));
  }
}
