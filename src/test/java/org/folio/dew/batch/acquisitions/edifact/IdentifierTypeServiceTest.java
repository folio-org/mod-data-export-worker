package org.folio.dew.batch.acquisitions.edifact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;

import org.folio.dew.BaseBatchTest;
import org.folio.dew.batch.acquisitions.edifact.services.IdentifierTypeService;
import org.folio.dew.client.IdentifierTypeClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.fasterxml.jackson.core.JsonProcessingException;


class IdentifierTypeServiceTest extends BaseBatchTest {
  @Autowired
  private IdentifierTypeService identifierTypeService;
  @MockBean
  private IdentifierTypeClient client;

  @Test
  void getIdentifierTypeName() {
    String identifierTypeName = identifierTypeService.getIdentifierTypeName("8261054f-be78-422d-bd51-4ed9f33c3422");
    assertEquals("", identifierTypeName);
  }

  @Test
  void getIdentifierTypeNameFromJson() throws JsonProcessingException {
    Mockito.when(client.getIdentifierType(anyString()))
      .thenReturn(objectMapper.readTree("{\"name\": \"ISSN\"}"));
    String identifierTypeName = identifierTypeService.getIdentifierTypeName("913300b2-03ed-469a-8179-c1092c991227");
    assertEquals("ISSN", identifierTypeName);
  }
}
