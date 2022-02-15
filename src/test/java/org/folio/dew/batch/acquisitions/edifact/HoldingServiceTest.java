package org.folio.dew.batch.acquisitions.edifact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

import org.folio.dew.BaseBatchTest;
import org.folio.dew.batch.acquisitions.edifact.services.HoldingService;
import org.folio.dew.client.HoldingClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.fasterxml.jackson.core.JsonProcessingException;


class HoldingServiceTest extends BaseBatchTest {
  @Autowired
  private HoldingService holdingService;
  @MockBean
  private HoldingClient client;


  @Test
  void getPermanentLocationIdFromJson() throws JsonProcessingException {
    var holdingJson = objectMapper.readTree("{\"permanentLocationId\": \"b241764c-1466-4e1d-a028-1a3684a5da87\"}");
    doReturn(holdingJson).when(client).getHoldingById(anyString());

    String locationId = holdingService.getPermanentLocationByHoldingId("65032151-39a5-4cef-8810-5350eb316300");
    assertEquals("b241764c-1466-4e1d-a028-1a3684a5da87", locationId);
  }
}
