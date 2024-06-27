package org.folio.dew.service;

import static org.folio.dew.service.FolioExecutionContextManager.X_OKAPI_TENANT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.folio.dew.client.InstanceClient;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class HoldingsReferenceServiceTest {
  @Mock
  private InstanceClient instanceClient;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @InjectMocks
  private HoldingsReferenceService service;

  @ParameterizedTest
  @CsvSource(value = {
    "titleOnly.json;Sample title",
    "titlePublisher.json;Sample title. Publisher",
    "titleDate.json;Sample title. , 2023",
    "titlePublisherDate.json;Sample title. Publisher, 2023"}, delimiter = ';')
  @SneakyThrows
  void shouldFormatInstanceTitle(String fileName, String expected) {
    var mapper = new ObjectMapper();
    var path = "src/test/resources/samples/" + fileName;
    var resultJson = mapper.readTree(new File(path));

    when(folioExecutionContext.getAllHeaders()).thenReturn(Map.of(X_OKAPI_TENANT, List.of("original")));
    when(instanceClient.getInstanceJsonById(anyString())).thenReturn(resultJson);

    var actual = service.getInstanceTitleById(UUID.randomUUID().toString(), "tenant");

    assertEquals(actual, expected);
  }
}
