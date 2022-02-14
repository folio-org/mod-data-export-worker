package org.folio.dew.batch.acquisitions.edifact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;

import org.folio.dew.BaseBatchTest;
import org.folio.dew.batch.acquisitions.edifact.client.MaterialTypeClient;
import org.folio.dew.batch.acquisitions.edifact.services.MaterialTypeService;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;


class MaterialTypeServiceTest extends BaseBatchTest {
  @Autowired
  private MaterialTypeService materialTypeService;
  @MockBean
  private MaterialTypeClient client;

  @Test
  void getMaterialTypeName() {
    String materialTypeName = materialTypeService.getMaterialTypeName("1a54b431-2e4f-452d-9cae-9cee66c9a892");
    assertEquals("", materialTypeName);
  }

  @Test
  void getMaterialTypeNameFromJson() {
    Mockito.when(client.getMaterialType(anyString()))
      .thenReturn(new JSONObject("{\"name\": \"Book\"}"));
    String materialTypeName = materialTypeService.getMaterialTypeName("1a54b431-2e4f-452d-9cae-9cee66c9a892");
    assertEquals("Book", materialTypeName);
  }
}
