package org.folio.dew.batch.acquisitions.mapper;

import static org.folio.dew.utils.TestUtils.getMockData;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;

import org.folio.dew.config.JacksonConfiguration;
import org.folio.dew.domain.dto.CompositePurchaseOrder;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderCsvMapperTest {

  private ObjectMapper objectMapper;
  private ExportResourceMapper orderCsvMapper;

  @BeforeEach
  void setUp() {
    orderCsvMapper = new OrderCsvMapper();
    objectMapper = new JacksonConfiguration().objectMapper();
  }

  @Test
  void testConvertForExportCsv() throws Exception {
    String jobName = "123456789012345";
    var compPO = objectMapper.readValue(getMockData("edifact/acquisitions/composite_purchase_order.json"), CompositePurchaseOrder.class);
    var ediConfig = getTestEdiConfig();

    String csvOutput = orderCsvMapper.convertForExport(List.of(compPO), List.of(), ediConfig, jobName);

    assertFalse(csvOutput.isEmpty());
    String[] lines = csvOutput.split("\n");
    assertEquals(2, lines.length);

    // Validate header
    assertEquals("POL number,Order number,Vendor order number,Account number,Title,Publisher,Publication date,"
      + "Product IDs,Quantity,Unit price,Estimated price,Currency,Fund codes,Contributors,Rush", lines[0]);

    // Validate data row contains expected values
    assertTrue(lines[1].contains("10000-1"));
    assertTrue(lines[1].contains("10000"));
    assertTrue(lines[1].contains("ORD1000"));
    assertTrue(lines[1].contains("BRXXXXX-01"));
    assertTrue(lines[1].contains("Palgrave Macmillan"));
    assertTrue(lines[1].contains("9783319643991"));
    assertTrue(lines[1].contains("USD"));
    assertTrue(lines[1].contains("USHIST"));
    assertTrue(lines[1].contains("false"));
  }

  @Test
  void testConvertForExportCsvMultipleOrders() throws Exception {
    String jobName = "123456789012345";
    var compPO1 = objectMapper.readValue(getMockData("edifact/acquisitions/composite_purchase_order.json"), CompositePurchaseOrder.class);
    var compPO2 = objectMapper.readValue(getMockData("edifact/acquisitions/comprehensive_composite_purchase_order.json"), CompositePurchaseOrder.class);
    var ediConfig = getTestEdiConfig();

    String csvOutput = orderCsvMapper.convertForExport(List.of(compPO1, compPO2), List.of(), ediConfig, jobName);

    assertFalse(csvOutput.isEmpty());
    String[] lines = csvOutput.split("\n");
    // Header + lines from both orders (compPO2 has 2 PO lines)
    assertEquals(1 + 1 + compPO2.getPoLines().size(), lines.length);
  }

  @Test
  void testConvertForExportCsvMinimalOrder() throws Exception {
    String jobName = "123456789012345";
    var compPO = objectMapper.readValue(getMockData("edifact/acquisitions/minimalistic_composite_purchase_order.json"), CompositePurchaseOrder.class);
    var ediConfig = getTestEdiConfig();

    String csvOutput = orderCsvMapper.convertForExport(List.of(compPO), List.of(), ediConfig, jobName);

    assertFalse(csvOutput.isEmpty());
    String[] lines = csvOutput.split("\n");
    assertEquals(1 + compPO.getPoLines().size(), lines.length);
  }

  private VendorEdiOrdersExportConfig getTestEdiConfig() throws IOException {
    return objectMapper.readValue(getMockData("edifact/acquisitions/vendorEdiOrdersExportConfig.json"), VendorEdiOrdersExportConfig.class);
  }

}
