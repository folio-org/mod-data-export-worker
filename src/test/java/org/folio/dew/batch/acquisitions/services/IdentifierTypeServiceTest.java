package org.folio.dew.batch.acquisitions.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.folio.dew.client.IdentifierTypeClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class IdentifierTypeServiceTest {

  @InjectMocks
  private IdentifierTypeService identifierTypeService;
  @Mock
  private IdentifierTypeClient client;
  @Spy
  private ObjectMapper objectMapper;

  @Test
  void getIdentifierTypeName() {
    String identifierTypeName = identifierTypeService.getIdentifierTypeName("8261054f-be78-422d-bd51-4ed9f33c3422");
    assertEquals("", identifierTypeName);
  }

  @Test
  void getIdentifierTypeNameFromJson() throws JsonProcessingException {
    var identifierTypeJson = objectMapper.readTree("{\"name\": \"ISSN\"}");
    when(client.getIdentifierType(anyString())).thenReturn(identifierTypeJson);
    String identifierTypeName = identifierTypeService.getIdentifierTypeName("913300b2-03ed-469a-8179-c1092c991227");
    assertEquals("ISSN", identifierTypeName);
  }
}
