package org.folio.dew.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.folio.dew.BaseBatchTest;
import org.folio.dew.client.InstanceClient;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.folio.dew.service.FolioExecutionContextManager.X_OKAPI_TENANT;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;


class HoldingsReferenceServiceTest extends BaseBatchTest {

  @Autowired
  private HoldingsReferenceService service;

  @ParameterizedTest
  @CsvSource(value = {
    "titleOnly.json;Sample title",
    "titlePublisher.json;Sample title. Publisher",
    "titleDate.json;Sample title. , 2023",
    "titlePublisherDate.json;Sample title. Publisher, 2023"}, delimiter = ';')
  @SneakyThrows
  void shouldFormatInstanceTitle(String fileName, String expected) {
    Map<String, Collection<String>> headers = new HashMap<>();
    headers.put(X_OKAPI_TENANT,  List.of("original"));
    var mapper = new ObjectMapper();
    var path = "src/test/resources/samples/" + fileName;
    var resultJson = mapper.readTree(new File(path));

    when(instanceClient.getInstanceJsonById(anyString())).thenReturn(resultJson);

    var actual = service.getInstanceTitleById(UUID.randomUUID().toString(), "tenant");

    assertEquals(actual, expected);
  }
}
