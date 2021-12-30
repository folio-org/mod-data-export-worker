package org.folio.dew.batch.acquisitions.edifact;

import org.folio.dew.BaseBatchTest;
import org.folio.dew.batch.acquisitions.edifact.services.MaterialTypeService;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;


class MaterialTypeServiceTest extends BaseBatchTest {
  @Autowired
  private MaterialTypeService materialTypeService;

  @Test
  void getMaterialTYpe() {
    String materialType = materialTypeService.getMaterialTypeName("1a54b431-2e4f-452d-9cae-9cee66c9a892");
    Assert.assertEquals(materialType, "Book");
  }
}
