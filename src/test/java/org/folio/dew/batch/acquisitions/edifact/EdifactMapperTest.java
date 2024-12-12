package org.folio.dew.batch.acquisitions.edifact;

import static org.folio.dew.domain.dto.ExportType.CLAIMS;
import static org.folio.dew.domain.dto.ExportType.EDIFACT_ORDERS_EXPORT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.any;
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
import java.util.Map;
import java.util.stream.Stream;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.folio.dew.batch.acquisitions.edifact.mapper.converter.CompOrderEdiConverter;
import org.folio.dew.batch.acquisitions.edifact.mapper.converter.CompPoLineEdiConverter;
import org.folio.dew.batch.acquisitions.edifact.mapper.EdifactMapper;
import org.folio.dew.batch.acquisitions.edifact.services.ConfigurationService;
import org.folio.dew.batch.acquisitions.edifact.services.ExpenseClassService;
import org.folio.dew.batch.acquisitions.edifact.services.HoldingService;
import org.folio.dew.batch.acquisitions.edifact.services.IdentifierTypeService;
import org.folio.dew.batch.acquisitions.edifact.services.LocationService;
import org.folio.dew.batch.acquisitions.edifact.services.MaterialTypeService;
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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@Log4j2
@ExtendWith(MockitoExtension.class)
class EdifactMapperTest {

  private static final Map<ExportType, String> EXPORT_EDI_PATHS = Map.of(
    EDIFACT_ORDERS_EXPORT,"edifact/acquisitions/edifact_orders_result.edi",
    CLAIMS, "edifact/acquisitions/edifact_claims_result.edi"
  );

  private ObjectMapper objectMapper;
  private EdifactMapper edifactMapper;

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
    var compositePOLineConverter = new CompPoLineEdiConverter(identifierTypeService, materialTypeService, expenseClassService, locationService, holdingService);
    var compositePOConverter = new CompOrderEdiConverter(compositePOLineConverter, configurationService);
    edifactMapper = new EdifactMapper(compositePOConverter);
    objectMapper = new JacksonConfiguration().objectMapper();
  }

  @ParameterizedTest
  @EnumSource(value = ExportType.class, names = {"EDIFACT_ORDERS_EXPORT", "CLAIMS"})
  void convertOrdersToEdifact(ExportType type) throws Exception {
    String jobName = "123456789012345";
    String fileIdExpected = "23456789012345";
    List<CompositePurchaseOrder> compPOs = getTestOrdersFromJson(type);
    List<Piece> pieces = getTestPiecesFromJson(type);

    serviceMocks();

    String ediOrder;
    if (type == EDIFACT_ORDERS_EXPORT) {
      ediOrder = edifactMapper.convertForExport(compPOs, List.of(), getTestEdiConfig(), jobName);
    } else {
      var piecePoLineIds = pieces.stream().map(Piece::getPoLineId).toList();
      compPOs = compPOs.stream()
        .peek(po -> po.getCompositePoLines().stream().filter(line -> piecePoLineIds.contains(line.getId())).toList())
        .filter(po -> !po.getCompositePoLines().isEmpty())
        .toList();
      ediOrder = edifactMapper.convertForExport(compPOs, pieces, getTestEdiConfig(), jobName);
    }

    assertFalse(ediOrder.isEmpty());
    validateEdifactOrders(type, ediOrder, fileIdExpected);
  }

  @ParameterizedTest
  @EnumSource(value = ExportType.class, names = {"EDIFACT_ORDERS_EXPORT", "CLAIMS"})
  void convertOrdersToEdifactByteArray(ExportType type) throws Exception {
    String jobName = "12345";
    List<CompositePurchaseOrder> compPOs = getTestOrdersFromJson(type);
    List<Piece> pieces = getTestPiecesFromJson(type);

    serviceMocks();

    byte[] ediOrder;
    if (type == EDIFACT_ORDERS_EXPORT) {
      ediOrder = edifactMapper.convertForExport(compPOs, List.of(), getTestEdiConfig(), jobName).getBytes(StandardCharsets.UTF_8);
    } else {
      ediOrder = edifactMapper.convertForExport(compPOs, pieces, getTestEdiConfig(), jobName).getBytes(StandardCharsets.UTF_8);
    }

    assertNotNull(ediOrder);
    validateEdifactOrders(type, new String(ediOrder), jobName);
  }

  private VendorEdiOrdersExportConfig getTestEdiConfig() throws IOException {
    return objectMapper.readValue(getMockData("edifact/acquisitions/vendorEdiOrdersExportConfig.json"), VendorEdiOrdersExportConfig.class);
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
    return compPOs;
  }


  private List<Piece> getTestPiecesFromJson(ExportType type) throws IOException {
    return type == EDIFACT_ORDERS_EXPORT
      ? List.of()
      : objectMapper.readValue(getMockData("edifact/acquisitions/pieces_collection_mixed.json"), PieceCollection.class).getPieces();
  }

  public static String getMockData(String path) throws IOException {
    try (InputStream resourceAsStream = EdifactMapperTest.class.getClassLoader().getResourceAsStream(path)) {
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
    Mockito.when(configurationService.getAddressConfig(any()))
      .thenReturn("Bockenheimer Landstr. 134-13");
  }

  private void validateEdifactOrders(ExportType type, String ediOrder, String fileId) throws IOException {
    log.info("Generated EDI file:\n{}", ediOrder);
    String ediOrderExpected = getMockData(EXPORT_EDI_PATHS.get(type)).replaceAll("\\{fileId}", fileId);
    String ediOrderWithRemovedDateTime = ediOrder.replaceFirst("\\d{6}:\\d{4}\\+", "ddmmyy:hhmm+");
    assertEquals(ediOrderExpected, ediOrderWithRemovedDateTime);
  }
}
