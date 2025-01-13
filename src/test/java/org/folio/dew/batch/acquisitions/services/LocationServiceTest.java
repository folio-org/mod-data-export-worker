package org.folio.dew.batch.acquisitions.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.folio.dew.BaseBatchTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;


class LocationServiceTest extends BaseBatchTest {
  @Autowired
  private LocationService locationService;

  @Test
  void getLOcationCode() {
    String locationCode = locationService.getLocationCodeById("b241764c-1466-4e1d-a028-1a3684a5da87");
    assertEquals("KU/CC/DI/P", locationCode);
  }
}
