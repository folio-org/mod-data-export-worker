package org.folio.dew.batch.acquisitions.edifact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;

import org.folio.dew.BaseBatchTest;
import org.folio.dew.batch.acquisitions.edifact.client.IdentifierTypeClient;
import org.folio.dew.batch.acquisitions.edifact.services.IdentifierTypeService;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;


class IdentifierTypeServiceTest extends BaseBatchTest {
  @Autowired
  private IdentifierTypeService identifierTypeService;
  @MockBean
  private IdentifierTypeClient client;

  @Test
  void getIdentifierTypeName() {
    String identifierTypeName = identifierTypeService.getIdentifierTypeName("8261054f-be78-422d-bd51-4ed9f33c3422");
    assertEquals("ISBN", identifierTypeName);
  }

  @Test
  void getIdentifierTypeNameFromJson() {
    Mockito.when(client.getIdentifierType(anyString()))
      .thenReturn(new JSONObject("{\"name\": \"ISSN\"}"));
    String identifierTypeName = identifierTypeService.getIdentifierTypeName("913300b2-03ed-469a-8179-c1092c991227");
    assertEquals("ISSN", identifierTypeName);
  }
}
