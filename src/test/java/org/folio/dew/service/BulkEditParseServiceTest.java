package org.folio.dew.service;

import org.folio.dew.BaseBatchTest;
import org.folio.dew.domain.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BulkEditParseServiceTest extends BaseBatchTest {
  @Autowired
  private BulkEditParseService bulkEditParseService;

  @ParameterizedTest
  @ValueSource(strings = {"", " "})
  void shouldIgnoreBlankBarcodeAndExternalSystemId(String val) {
    var userFormat = UserFormat.builder()
      .patronGroup("PatronGroup")
      .externalSystemId(val)
      .barcode(val)
      .active("true")
      .departments("")
      .proxyFor("")
      .addresses("")
      .build();

    assertThat(bulkEditParseService.mapUserFormatToUser(userFormat).getExternalSystemId()).isNull();
    assertThat(bulkEditParseService.mapUserFormatToUser(userFormat).getBarcode()).isNull();
  }

  @Test
  void shouldReturnInitialIdsForWrongReferenceIdsWhenMappingToUser() {
    var userFormat = UserFormat.builder()
      .active("true")
      .patronGroup("staff")
      .addresses(";;;;;;;false;db541cda-fcc7-403b-8077-3613f3244901")
      .preferredContactTypeId("002")
      .departments("103aee0f-c5f6-44de-94aa-74093f0e45d9")
      .build();

    var expectedUser = new User()
      .personal(new Personal().addresses(Collections.singletonList(new Address().addressTypeId("db541cda-fcc7-403b-8077-3613f3244901"))))
      .patronGroup("3684a786-6671-4268-8ed0-9db82ebca60b")
      .departments(Set.of(UUID.fromString("103aee0f-c5f6-44de-94aa-74093f0e45d9")));

    var actualUser = bulkEditParseService.mapUserFormatToUser(userFormat);

    assertEquals(expectedUser.getPersonal().getAddresses().get(0).getAddressTypeId(), actualUser.getPersonal().getAddresses().get(0).getAddressTypeId());
    assertEquals(expectedUser.getDepartments(), actualUser.getDepartments());
  }
}
