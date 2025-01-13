package org.folio.dew.batch.acquisitions.mapper;

import static org.folio.dew.domain.dto.ExportType.CLAIMS;
import static org.folio.dew.utils.TestUtils.getMockData;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.log4j.Log4j2;

import org.folio.dew.batch.acquisitions.services.OrdersService;
import org.folio.dew.config.JacksonConfiguration;
import org.folio.dew.domain.dto.CompositePurchaseOrder;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.OrdersTitle;
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
class CsvMapperTest {

  private static final Map<ExportType, String> EXPORT_CSV_PATHS = Map.of(
    CLAIMS, "edifact/acquisitions/csv_claims_result.csv"
  );

  private ObjectMapper objectMapper;
  private ExportResourceMapper csvMapper;

  @Mock
  private OrdersService ordersService;

  @BeforeEach
  void setUp() {
    csvMapper = new CsvMapper(ordersService);
    objectMapper = new JacksonConfiguration().objectMapper();

    when(ordersService.getTitleById(anyString())).thenReturn(new OrdersTitle().title("Test title"));
  }

  @ParameterizedTest
  @EnumSource(value = ExportType.class, names = {"CLAIMS"})
  void testConvertForExportCsv(ExportType type) throws Exception {
    String jobName = "123456789012345";
    List<CompositePurchaseOrder> compPOs = getTestOrdersFromJson(type);
    List<Piece> pieces = getTestPiecesFromJson(type);

    String csvOutput = csvMapper.convertForExport(compPOs, pieces, getTestEdiConfig(), jobName);

    assertFalse(csvOutput.isEmpty());
    validateCsvOutput(type, csvOutput);

    byte[] csvOutputBytes = csvOutput.getBytes(StandardCharsets.UTF_8);
    assertNotNull(csvOutputBytes);
    validateCsvOutput(type, new String(csvOutputBytes));
  }

  private VendorEdiOrdersExportConfig getTestEdiConfig() throws IOException {
    return objectMapper.readValue(getMockData("edifact/acquisitions/vendorEdiOrdersExportConfig.json"), VendorEdiOrdersExportConfig.class);
  }

  private List<CompositePurchaseOrder> getTestOrdersFromJson(ExportType type) throws IOException {
    List<CompositePurchaseOrder> compPOs = new ArrayList<>();
    if (type == CLAIMS) {
      compPOs.add(objectMapper.readValue(getMockData("edifact/acquisitions/composite_purchase_order.json"), CompositePurchaseOrder.class));
      compPOs.add(objectMapper.readValue(getMockData("edifact/acquisitions/comprehensive_composite_purchase_order.json"), CompositePurchaseOrder.class));
      compPOs.add(objectMapper.readValue(getMockData("edifact/acquisitions/minimalistic_composite_purchase_order.json"), CompositePurchaseOrder.class));
    }
    return compPOs;
  }

  private List<Piece> getTestPiecesFromJson(ExportType type) throws IOException {
    return type == CLAIMS
      ? objectMapper.readValue(getMockData("edifact/acquisitions/pieces_collection_mixed.json"), PieceCollection.class).getPieces()
      : List.of();
  }

  private void validateCsvOutput(ExportType type, String csvOutput) throws IOException {
    log.info("Generated CSV file:\n{}", csvOutput);
    String csvExpected = getMockData(EXPORT_CSV_PATHS.get(type));
    assertEquals(csvExpected, csvOutput);
  }

}
