package org.folio.dew.batch.acquisitions.edifact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;

import org.folio.dew.BaseBatchTest;
import org.folio.dew.batch.acquisitions.edifact.client.HoldingClient;
import org.folio.dew.batch.acquisitions.edifact.services.HoldingService;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;


class HoldingServiceTest extends BaseBatchTest {
  @Autowired
  private HoldingService holdingService;
  @MockBean
  private HoldingClient client;

  @Test
  void getPermanentLocationId() {
    String locationId = holdingService.getPermanentLocationId("65032151-39a5-4cef-8810-5350eb316300");
    assertEquals("", locationId);
  }

  @Test
  void getPermanentLocationIdFromJson() {
    Mockito.when(client.getHolding(anyString()))
      .thenReturn(new JSONObject("{\"permanentLocationId\": \"b241764c-1466-4e1d-a028-1a3684a5da87\"}"));
    String locationId = holdingService.getPermanentLocationId("65032151-39a5-4cef-8810-5350eb316300");
    assertEquals("b241764c-1466-4e1d-a028-1a3684a5da87", locationId);
  }
}
