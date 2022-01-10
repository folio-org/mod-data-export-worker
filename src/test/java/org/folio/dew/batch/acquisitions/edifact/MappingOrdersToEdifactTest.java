package org.folio.dew.batch.acquisitions.edifact;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.folio.dew.domain.dto.CompositePurchaseOrder;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@SpringBootTest
@AutoConfigureMockMvc
class MappingOrdersToEdifactTest {
  @Autowired
  private MappingOrdersToEdifact mappingOrdersToEdifact;

  @Test void convertOrdersToEdifact() throws Exception {
    JSONObject jsonObject = new JSONObject(getMockData("edifact/acquisitions/composite_purchase_order.json"));
    ObjectMapper mapper = new ObjectMapper();
    CompositePurchaseOrder reqData  = mapper.readValue(jsonObject.toString(), CompositePurchaseOrder.class);

    List<CompositePurchaseOrder> compPOs = new ArrayList<>();
    compPOs.add(reqData);
    String ediOrder = mappingOrdersToEdifact.convertOrdersToEdifact(compPOs);
    assertFalse(ediOrder.isEmpty());
    System.out.println(ediOrder);
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
