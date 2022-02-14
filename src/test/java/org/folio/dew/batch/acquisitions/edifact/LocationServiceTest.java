package org.folio.dew.batch.acquisitions.edifact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;

import org.folio.dew.BaseBatchTest;
import org.folio.dew.batch.acquisitions.edifact.client.LocationClient;
import org.folio.dew.batch.acquisitions.edifact.services.LocationService;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;


class LocationServiceTest extends BaseBatchTest {
  @Autowired
  private LocationService locationService;
  @MockBean
  private LocationClient client;

  @Test
  void getMaterialTypeName() {
    String locationCode = locationService.getLocationCode("b241764c-1466-4e1d-a028-1a3684a5da87");
    assertEquals("", locationCode);
  }

  @Test
  void getMaterialTypeNameFromJson() {
    Mockito.when(client.getLocation(anyString()))
      .thenReturn(new JSONObject("{\"code\": \"KU/CC/DI/P\"}"));
    String locationCode = locationService.getLocationCode("b241764c-1466-4e1d-a028-1a3684a5da87");
    assertEquals("KU/CC/DI/P", locationCode);
  }
}
