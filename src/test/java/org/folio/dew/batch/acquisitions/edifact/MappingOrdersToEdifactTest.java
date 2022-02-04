package org.folio.dew.batch.acquisitions.edifact;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.folio.dew.batch.acquisitions.edifact.services.MaterialTypeService;
import org.folio.dew.domain.dto.CompositePurchaseOrder;
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
  private MappingOrdersToEdifact mappingOrdersToEdifact;
  @MockBean
  private MaterialTypeService materialTypeService;

  @Test void convertOrdersToEdifact() throws Exception {
    ObjectMapper mapper = new ObjectMapper();

    JSONObject firstJson = new JSONObject(getMockData("edifact/acquisitions/composite_purchase_order.json"));
    CompositePurchaseOrder compPo = mapper.readValue(firstJson.toString(), CompositePurchaseOrder.class);

    JSONObject secondJson = new JSONObject(getMockData("edifact/acquisitions/comprehensive_composite_purchase_order.json"));
    CompositePurchaseOrder comprehensiveCompPo = mapper.readValue(secondJson.toString(), CompositePurchaseOrder.class);

    //TODO finish minimalistic po json (omitting as many fields as possible) and add it here

    Mockito.when(materialTypeService.getMaterialTypeName(anyString()))
      .thenReturn("Book");

    List<CompositePurchaseOrder> compPOs = new ArrayList<>();
    compPOs.add(compPo);
    compPOs.add(comprehensiveCompPo);
    String ediOrder = mappingOrdersToEdifact.convertOrdersToEdifact(compPOs);
    assertFalse(ediOrder.isEmpty());
    log.info(ediOrder);
  }

  @Test void convertOrdersToEdifactByteArray() throws Exception {
    JSONObject jsonObject = new JSONObject(getMockData("edifact/acquisitions/composite_purchase_order.json"));
    ObjectMapper mapper = new ObjectMapper();
    CompositePurchaseOrder reqData  = mapper.readValue(jsonObject.toString(), CompositePurchaseOrder.class);

    List<CompositePurchaseOrder> compPOs = new ArrayList<>();
    compPOs.add(reqData);
    byte[] ediOrder = mappingOrdersToEdifact.convertOrdersToEdifactArray(compPOs);
    assertNotNull(ediOrder);
    log.info(Arrays.toString(ediOrder));
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
