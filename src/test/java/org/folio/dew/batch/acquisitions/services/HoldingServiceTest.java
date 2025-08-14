package org.folio.dew.batch.acquisitions.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.folio.dew.client.HoldingClient;
import org.folio.dew.domain.dto.Location;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.utils.FolioExecutionContextUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class HoldingServiceTest {

  @InjectMocks
  private HoldingService holdingService;
  @Mock
  private HoldingClient client;
  @Mock
  private FolioModuleMetadata folioModuleMetadata;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Spy
  private ObjectMapper objectMapper;

  @Test
  void getPermanentLocationIdFromJson() throws JsonProcessingException {
    var holdingJson = objectMapper.readTree("{\"permanentLocationId\": \"b241764c-1466-4e1d-a028-1a3684a5da87\"}");
    doReturn(holdingJson).when(client).getHoldingById(anyString());

    String locationId = holdingService.getPermanentLocationByHoldingId("65032151-39a5-4cef-8810-5350eb316300", null);
    assertEquals("b241764c-1466-4e1d-a028-1a3684a5da87", locationId);
  }

  @Test
  void getPermanentLocationIdFromJsonWithTenantId() throws JsonProcessingException {
    var holdingJson = objectMapper.readTree("{\"permanentLocationId\": \"b241764c-1466-4e1d-a028-1a3684a5da87\"}");
    doReturn(holdingJson).when(client).getHoldingById(anyString());

    try (MockedStatic<FolioExecutionContextUtils> mocked = mockStatic(FolioExecutionContextUtils.class)) {
      mocked.when(() -> FolioExecutionContextUtils.prepareContextForTenant(anyString(), any(FolioModuleMetadata.class), any(FolioExecutionContext.class)))
        .thenReturn(folioExecutionContext);

      Location location = new Location()
        .holdingId(UUID.fromString("65032151-39a5-4cef-8810-5350eb316300"))
        .tenantId("test_tenant");
      String locationId = holdingService.getPermanentLocationByHoldingId(location.getHoldingId().toString(), location.getTenantId());
      assertEquals("b241764c-1466-4e1d-a028-1a3684a5da87", locationId);
    }
  }

  @Test
  void getInstanceIdByHoldingShouldReturnCorrectInstanceId() throws JsonProcessingException {
    var holdingJson = objectMapper.readTree("{\"instanceId\": \"123\"}");
    String actual = holdingService.getInstanceIdByHolding(holdingJson);
    assertEquals("123", actual);
  }

  @Test
  void getInstanceIdByHoldingShouldReturnCorrectEmptyStringWhenNull() {
    String actual = holdingService.getInstanceIdByHolding(null);
    assertTrue(actual.isEmpty());
  }

  @Test
  void getInstanceIdByHoldingShouldReturnCorrectEmptyStringWhenHoldingIsEmpty() throws JsonProcessingException {
    var holdingJson = objectMapper.readTree("");
    String actual = holdingService.getInstanceIdByHolding(holdingJson);
    assertTrue(actual.isEmpty());
  }
}
