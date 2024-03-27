package org.folio.dew.batch.acquisitions.edifact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.folio.dew.BaseBatchTest;
import org.folio.dew.batch.acquisitions.edifact.services.ConfigurationService;
import org.folio.dew.batch.acquisitions.edifact.services.ExpenseClassService;
import org.folio.dew.batch.acquisitions.edifact.services.HoldingService;
import org.folio.dew.batch.acquisitions.edifact.services.IdentifierTypeService;
import org.folio.dew.batch.acquisitions.edifact.services.LocationService;
import org.folio.dew.batch.acquisitions.edifact.services.MaterialTypeService;
import org.folio.dew.domain.dto.CompositePurchaseOrder;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@Log4j2
@ExtendWith(MockitoExtension.class)
class MappingOrdersToEdifactTest extends BaseBatchTest {
  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private PurchaseOrdersToEdifactMapper purchaseOrdersToEdifactMapper;
  @MockBean
  private IdentifierTypeService identifierTypeService;
  @MockBean
  private MaterialTypeService materialTypeService;
  @MockBean
  private ExpenseClassService expenseClassService;
  @MockBean
  private LocationService locationService;
  @MockBean
  private HoldingService holdingService;
  @MockBean
  private ConfigurationService configurationService;

  @Test
  void convertOrdersToEdifact() throws Exception {
    String jobName = "123456789012345";
    String fileIdExpected = "23456789012345";
    List<CompositePurchaseOrder> compPOs = getTestOrdersFromJson();

    serviceMocks();

    String ediOrder = purchaseOrdersToEdifactMapper.convertOrdersToEdifact(compPOs, getTestEdiConfig(), jobName);
    log.info(ediOrder);
    assertFalse(ediOrder.isEmpty());
    validateEdifactOrders(ediOrder, fileIdExpected);
  }

  @Test
  void convertOrdersToEdifactByteArray() throws Exception {
    String jobName = "12345";
    List<CompositePurchaseOrder> compPOs = getTestOrdersFromJson();

    serviceMocks();

    byte[] ediOrder = purchaseOrdersToEdifactMapper.convertOrdersToEdifactArray(compPOs, getTestEdiConfig(), jobName);
    assertNotNull(ediOrder);
    String ediOrderString = new String(ediOrder);
    log.info(ediOrderString);
    validateEdifactOrders(ediOrderString, jobName);
  }

  private VendorEdiOrdersExportConfig getTestEdiConfig() throws IOException {
    return objectMapper.readValue(getMockData("edifact/acquisitions/vendorEdiOrdersExportConfig.json"), VendorEdiOrdersExportConfig.class);
  }

  private List<CompositePurchaseOrder> getTestOrdersFromJson() throws IOException {

    CompositePurchaseOrder compPo = objectMapper.readValue(getMockData("edifact/acquisitions/composite_purchase_order.json"), CompositePurchaseOrder.class);

    CompositePurchaseOrder comprehensiveCompPo = objectMapper.readValue(getMockData("edifact/acquisitions/comprehensive_composite_purchase_order.json"), CompositePurchaseOrder.class);

    CompositePurchaseOrder minimalisticCompPo = objectMapper.readValue(getMockData("edifact/acquisitions/minimalistic_composite_purchase_order.json"), CompositePurchaseOrder.class);

    CompositePurchaseOrder compPoWithEmptyVendorAccount = objectMapper.readValue(getMockData("edifact/acquisitions/purchase_order_empty_vendor_account.json"), CompositePurchaseOrder.class);

    CompositePurchaseOrder compPoWithNonEANProductIds = objectMapper.readValue(getMockData("edifact/acquisitions/purchase_order_non_ean_product_ids.json"), CompositePurchaseOrder.class);

    CompositePurchaseOrder compPoTitleWithEscapeChars = objectMapper.readValue(getMockData("edifact/acquisitions/purchase_order_title_with_escape_chars.json"), CompositePurchaseOrder.class);

    List<CompositePurchaseOrder> compPOs = new ArrayList<>();
    compPOs.add(compPo);
    compPOs.add(comprehensiveCompPo);
    compPOs.add(minimalisticCompPo);
    compPOs.add(compPoWithEmptyVendorAccount);
    compPOs.add(compPoWithNonEANProductIds);
    compPOs.add(compPoTitleWithEscapeChars);
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

  private void serviceMocks() {
    Mockito.when(identifierTypeService.getIdentifierTypeName("8261054f-be78-422d-bd51-4ed9f33c3422"))
      .thenReturn("ISSN", "ISMN", "ISBN", "ISSN", "ISMN", "ISBN");
    Mockito.when(identifierTypeService.getIdentifierTypeName(not(eq("8261054f-be78-422d-bd51-4ed9f33c3422"))))
      .thenReturn("Publisher or distributor number");
    Mockito.when(materialTypeService.getMaterialTypeName(anyString()))
      .thenReturn("Book");
    Mockito.when(expenseClassService.getExpenseClassCode(anyString()))
      .thenReturn("Elec");
    Mockito.when(locationService.getLocationCodeById(anyString()))
      .thenReturn("KU/CC/DI/M");
    Mockito.when(holdingService.getPermanentLocationByHoldingId(anyString()))
      .thenReturn("fcd64ce1-6995-48f0-840e-89ffa2288371");
    Mockito.when(configurationService.getSystemCurrency())
      .thenReturn("GBP");
  }

  private void validateEdifactOrders(String ediOrder, String fileId) throws IOException {
    String ediOrderExpected = getMockData("edifact/acquisitions/edifact_orders_result.edi")
      .replaceAll("\\{fileId}", fileId);

    String ediOrderWithRemovedDateTime = ediOrder.replaceFirst("\\d{6}:\\d{4}\\+", "ddmmyy:hhmm+");
    assertEquals(ediOrderExpected, ediOrderWithRemovedDateTime);
  }
}
