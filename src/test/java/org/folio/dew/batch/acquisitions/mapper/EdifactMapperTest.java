package org.folio.dew.batch.acquisitions.mapper;

import static org.folio.dew.domain.dto.ExportType.CLAIMS;
import static org.folio.dew.domain.dto.ExportType.EDIFACT_ORDERS_EXPORT;
import static org.folio.dew.domain.dto.VendorEdiOrdersExportConfig.IntegrationTypeEnum.CLAIMING;
import static org.folio.dew.domain.dto.VendorEdiOrdersExportConfig.IntegrationTypeEnum.ORDERING;
import static org.folio.dew.utils.TestUtils.getMockData;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.log4j.Log4j2;

import org.folio.dew.batch.acquisitions.mapper.converter.CompOrderEdiConverter;
import org.folio.dew.batch.acquisitions.mapper.converter.PoLineEdiConverter;
import org.folio.dew.batch.acquisitions.services.ConfigurationService;
import org.folio.dew.batch.acquisitions.services.ExpenseClassService;
import org.folio.dew.batch.acquisitions.services.HoldingService;
import org.folio.dew.batch.acquisitions.services.IdentifierTypeService;
import org.folio.dew.batch.acquisitions.services.LocationService;
import org.folio.dew.batch.acquisitions.services.MaterialTypeService;
import org.folio.dew.config.JacksonConfiguration;
import org.folio.dew.domain.dto.CompositePurchaseOrder;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.Piece;
import org.folio.dew.domain.dto.PieceCollection;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Log4j2
@ExtendWith(MockitoExtension.class)
class EdifactMapperTest {

  private static final Map<ExportType, String> EXPORT_EDI_PATHS = Map.of(
    EDIFACT_ORDERS_EXPORT,"edifact/acquisitions/edifact_orders_result.edi",
    CLAIMS, "edifact/acquisitions/edifact_claims_result.edi"
  );

  private ObjectMapper objectMapper;
  private ExportResourceMapper edifactMapper;

  @Mock
  private IdentifierTypeService identifierTypeService;
  @Mock
  private MaterialTypeService materialTypeService;
  @Mock
  private ExpenseClassService expenseClassService;
  @Mock
  private LocationService locationService;
  @Mock
  private HoldingService holdingService;
  @Mock
  private ConfigurationService configurationService;

  @BeforeEach
  void setUp() {
    var poLineConverter = new PoLineEdiConverter(identifierTypeService, materialTypeService, expenseClassService, locationService, holdingService);
    var compositePOConverter = new CompOrderEdiConverter(poLineConverter, configurationService);
    edifactMapper = new EdifactMapper(compositePOConverter);
    objectMapper = new JacksonConfiguration().objectMapper();

    when(identifierTypeService.getIdentifierTypeName("8261054f-be78-422d-bd51-4ed9f33c3422"))
      .thenReturn("ISSN", "ISMN", "ISBN", "ISSN", "ISMN", "ISBN");
    when(identifierTypeService.getIdentifierTypeName(not(eq("8261054f-be78-422d-bd51-4ed9f33c3422"))))
      .thenReturn("Publisher or distributor number");
    when(materialTypeService.getMaterialTypeName(anyString()))
      .thenReturn("Book");
    when(configurationService.getAddressConfig(any()))
      .thenReturn("Bockenheimer Landstr. 134-13");
  }

  @ParameterizedTest
  @EnumSource(value = ExportType.class, names = {"EDIFACT_ORDERS_EXPORT", "CLAIMS"})
  void convertOrdersToEdifact(ExportType type) throws Exception {
    String jobName = "123456789012345";
    String fileIdExpected = "23456789012345";
    List<CompositePurchaseOrder> compPOs = getTestOrdersFromJson(type);
    List<Piece> pieces = getTestPiecesFromJson(type);

    if (type == EDIFACT_ORDERS_EXPORT) {
      when(expenseClassService.getExpenseClassCode(anyString()))
        .thenReturn("Elec");
      when(locationService.getLocationCodeById(anyString()))
        .thenReturn("KU/CC/DI/M");
      when(holdingService.getPermanentLocationByHoldingId(anyString()))
        .thenReturn("fcd64ce1-6995-48f0-840e-89ffa2288371");
    }

    String ediOrder = edifactMapper.convertForExport(compPOs, pieces, getTestEdiConfig(type), jobName);

    assertFalse(ediOrder.isEmpty());
    validateEdifactOrders(type, ediOrder, fileIdExpected);

    byte[] ediOrderBytes = ediOrder.getBytes(StandardCharsets.UTF_8);
    assertNotNull(ediOrderBytes);
    validateEdifactOrders(type, ediOrder, fileIdExpected);
  }

  private VendorEdiOrdersExportConfig getTestEdiConfig(ExportType type) throws IOException {
    var exportConfig = objectMapper.readValue(getMockData("edifact/acquisitions/vendorEdiOrdersExportConfig.json"), VendorEdiOrdersExportConfig.class);
    exportConfig.setIntegrationType(type == EDIFACT_ORDERS_EXPORT ? ORDERING : CLAIMING);
    return exportConfig;
  }

  private List<CompositePurchaseOrder> getTestOrdersFromJson(ExportType type) throws IOException {
    List<CompositePurchaseOrder> compPOs = new ArrayList<>();
    compPOs.add(objectMapper.readValue(getMockData("edifact/acquisitions/composite_purchase_order.json"), CompositePurchaseOrder.class));
    compPOs.add(objectMapper.readValue(getMockData("edifact/acquisitions/comprehensive_composite_purchase_order.json"), CompositePurchaseOrder.class));
    compPOs.add(objectMapper.readValue(getMockData("edifact/acquisitions/minimalistic_composite_purchase_order.json"), CompositePurchaseOrder.class));
    if (type == EDIFACT_ORDERS_EXPORT) {
      compPOs.add(objectMapper.readValue(getMockData("edifact/acquisitions/purchase_order_empty_vendor_account.json"), CompositePurchaseOrder.class));
      compPOs.add(objectMapper.readValue(getMockData("edifact/acquisitions/purchase_order_non_ean_product_ids.json"), CompositePurchaseOrder.class));
      compPOs.add(objectMapper.readValue(getMockData("edifact/acquisitions/purchase_order_title_with_escape_chars.json"), CompositePurchaseOrder.class));
    }
    compPOs.forEach(compPO -> compPO.setDateOrdered(java.util.Date.from(LocalDate.of(2025, 5, 8)
      .atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())));
    return compPOs;
  }

  private List<Piece> getTestPiecesFromJson(ExportType type) throws IOException {
    return type == EDIFACT_ORDERS_EXPORT
      ? List.of()
      : objectMapper.readValue(getMockData("edifact/acquisitions/pieces_collection_mixed.json"), PieceCollection.class).getPieces();
  }

  private void validateEdifactOrders(ExportType type, String ediOrder, String fileId) throws IOException {
    log.info("Generated EDI file:\n{}", ediOrder);
    String ediOrderExpected = getMockData(EXPORT_EDI_PATHS.get(type)).replaceAll("\\{fileId}", fileId);
    String ediOrderWithRemovedDateTime = ediOrder.replaceFirst("\\d{6}:\\d{4}\\+", "ddmmyy:hhmm+");
    assertEquals(ediOrderExpected, ediOrderWithRemovedDateTime);
  }
}
