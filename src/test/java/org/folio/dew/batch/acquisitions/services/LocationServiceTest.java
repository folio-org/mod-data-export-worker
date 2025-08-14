package org.folio.dew.batch.acquisitions.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.folio.dew.client.LocationClient;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

  @InjectMocks
  private LocationService locationService;
  @Mock
  private LocationClient client;
  @Mock
  private FolioModuleMetadata folioModuleMetadata;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Spy
  private ObjectMapper objectMapper;

  @Test
  void getLocationCodeById() throws JsonProcessingException {
    var locationJson = objectMapper.readTree("{\"code\": \"KU/CC/DI/P\"}");
    doReturn(locationJson).when(client).getLocation(anyString());

    String locationCode = locationService.getLocationCodeById("b241764c-1466-4e1d-a028-1a3684a5da87", null);
    assertEquals("KU/CC/DI/P", locationCode);
  }

  @Test
  void getLocationCodeByIdWithTenant() throws JsonProcessingException {
    var locationJson = objectMapper.readTree("{\"code\": \"KU/CC/DI/P\"}");
    doReturn(locationJson).when(client).getLocation(anyString());

    try (MockedStatic<FolioExecutionContextUtils> mocked = mockStatic(FolioExecutionContextUtils.class)) {
      mocked.when(() -> FolioExecutionContextUtils.prepareContextForTenant(anyString(), any(FolioModuleMetadata.class), any(FolioExecutionContext.class)))
        .thenReturn(folioExecutionContext);

      String locationCode = locationService.getLocationCodeById("b241764c-1466-4e1d-a028-1a3684a5da87", "test_tenant");
      assertEquals("KU/CC/DI/P", locationCode);
    }
  }
}
