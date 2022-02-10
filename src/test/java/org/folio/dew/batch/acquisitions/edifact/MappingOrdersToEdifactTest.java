package org.folio.dew.batch.acquisitions.edifact;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.folio.dew.batch.acquisitions.edifact.services.MaterialTypeService;
import org.folio.dew.domain.dto.CompositePurchaseOrder;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;

@Log4j2
@SpringBootTest
@AutoConfigureMockMvc
@RunWith(MockitoJUnitRunner.class)
class MappingOrdersToEdifactTest {
  @Autowired
  private PurchaseOrdersToEdifactMapper purchaseOrdersToEdifactMapper;
  @MockBean
  private MaterialTypeService materialTypeService;

  @Test void convertOrdersToEdifact() throws Exception {
    List<CompositePurchaseOrder> compPOs = getTestOrdersFromJson();

    Mockito.when(materialTypeService.getMaterialTypeName(anyString()))
      .thenReturn("Book");

    String ediOrder = purchaseOrdersToEdifactMapper.convertOrdersToEdifact(compPOs, getTestEdiConfig());
    assertFalse(ediOrder.isEmpty());
    log.info(ediOrder);
  }

  @Test
  void convertOrdersToEdifactByteArray() throws Exception {
    List<CompositePurchaseOrder> compPOs = getTestOrdersFromJson();

    Mockito.when(materialTypeService.getMaterialTypeName(anyString()))
      .thenReturn("Book");

    byte[] ediOrder = purchaseOrdersToEdifactMapper.convertOrdersToEdifactArray(compPOs, getTestEdiConfig());
    assertNotNull(ediOrder);
    log.info(Arrays.toString(ediOrder));
  }

  private VendorEdiOrdersExportConfig getTestEdiConfig() throws IOException {
    ObjectMapper mapper = new ObjectMapper();

    JSONObject json = new JSONObject(getMockData("edifact/acquisitions/vendorEdiOrdersExportConfig.json"));
    return mapper.readValue(json.toString(), VendorEdiOrdersExportConfig.class);
  }

  private List<CompositePurchaseOrder> getTestOrdersFromJson() throws IOException {
    ObjectMapper mapper = new ObjectMapper();

    JSONObject json = new JSONObject(getMockData("edifact/acquisitions/composite_purchase_order.json"));
    CompositePurchaseOrder compPo = mapper.readValue(json.toString(), CompositePurchaseOrder.class);

    JSONObject comprehensiveJson = new JSONObject(getMockData("edifact/acquisitions/comprehensive_composite_purchase_order.json"));
    CompositePurchaseOrder comprehensiveCompPo = mapper.readValue(comprehensiveJson.toString(), CompositePurchaseOrder.class);

    JSONObject minimalisticJson = new JSONObject(getMockData("edifact/acquisitions/minimalistic_composite_purchase_order.json"));
    CompositePurchaseOrder minimalisticCompPo = mapper.readValue(minimalisticJson.toString(), CompositePurchaseOrder.class);

    List<CompositePurchaseOrder> compPOs = new ArrayList<>();
    compPOs.add(compPo);
    compPOs.add(comprehensiveCompPo);
    compPOs.add(minimalisticCompPo);
    return compPOs;
  }

  public static String getMockData(String path) throws IOException {
    try (InputStream resourceAsStream = MappingOrdersToEdifactTest.class.getClassLoader().getResourceAsStream(path)) {
      if (resourceAsStream != null) {
        return IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8);
      } else {
        StringBuilder sb = new StringBuilder();
        try (Stream<String> lines = Files.lines(Paths.get(path))) {
          lines.forEach(sb::append);
        }
        return sb.toString();
      }
    }
  }
}
