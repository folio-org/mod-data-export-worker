package org.folio.dew.service;

import org.folio.dew.BaseBatchTest;
import org.folio.dew.domain.dto.UserFormat;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

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
}
