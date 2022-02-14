package org.folio.dew.batch.acquisitions.edifact;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.folio.dew.BaseBatchTest;
import org.folio.dew.batch.acquisitions.edifact.services.LocationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;


class LocationServiceTest extends BaseBatchTest {
  @Autowired
  private LocationService locationService;

  @Test
  void getMaterialTypeName() {
    String locationCode = locationService.getLocationCode("b241764c-1466-4e1d-a028-1a3684a5da87");
    assertEquals("", locationCode);
  }
}
