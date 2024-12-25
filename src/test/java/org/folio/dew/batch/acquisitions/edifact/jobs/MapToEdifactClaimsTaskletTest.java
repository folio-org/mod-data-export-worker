package org.folio.dew.batch.acquisitions.edifact.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dew.batch.acquisitions.edifact.utils.ExportConfigFields.CLAIM_PIECE_IDS;
import static org.folio.dew.batch.acquisitions.edifact.utils.ExportConfigFields.INTEGRATION_TYPE;
import static org.folio.dew.batch.acquisitions.edifact.utils.ExportConfigFields.TRANSMISSION_METHOD;
import static org.folio.dew.domain.dto.VendorEdiOrdersExportConfig.IntegrationTypeEnum.CLAIMING;
import static org.folio.dew.domain.dto.VendorEdiOrdersExportConfig.TransmissionMethodEnum.FTP;
import static org.folio.dew.utils.QueryUtils.convertIdsToCqlQuery;
import static org.folio.dew.utils.TestUtils.getMockData;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.folio.dew.domain.dto.Piece;
import org.folio.dew.domain.dto.PieceCollection;
import org.folio.dew.domain.dto.PoLine;
import org.folio.dew.domain.dto.PoLineCollection;
import org.folio.dew.domain.dto.PurchaseOrder;
import org.folio.dew.domain.dto.PurchaseOrderCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.SneakyThrows;

class MapToEdifactClaimsTaskletTest extends MapToEdifactTaskletAbstractTest {

  private static final String SAMPLE_PIECES_PATH = "edifact/acquisitions/pieces_collection.json";

  @Autowired
  Job edifactClaimsExportJob;

  private List<PurchaseOrder> orders;
  private List<PoLine> poLines;
  private List<Piece> pieces;
  private List<String> pieceIds;

  @BeforeEach
  @SneakyThrows
  @Override
  protected void setUp() {
    super.setUp();
    edifactExportJob = edifactClaimsExportJob;
    orders = objectMapper.readValue(getMockData(SAMPLE_PURCHASE_ORDERS_PATH), PurchaseOrderCollection.class).getPurchaseOrders();
    poLines = objectMapper.readValue(getMockData(SAMPLE_PO_LINES_PATH), PoLineCollection.class).getPoLines();
    pieces = objectMapper.readValue(getMockData(SAMPLE_PIECES_PATH), PieceCollection.class).getPieces();

    pieceIds = pieces.stream().map(Piece::getId).toList();
    doReturn(pieces).when(ordersService).getPiecesByIdsAndReceivingStatus(pieceIds, Piece.ReceivingStatusEnum.CLAIM_SENT);
  }

  @Test
  void testEdifactClaimsExport() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(edifactExportJob);
    String poLineQuery = convertIdsToCqlQuery(pieces.stream().map(Piece::getPoLineId).toList());

    doReturn(poLines).when(ordersService).getPoLinesByQuery(poLineQuery);
    doReturn(orders).when(ordersService).getPurchaseOrdersByIds(anyList());
    doReturn("test1").when(edifactMapper).convertForExport(any(), any(), any(), anyString());

    var exportConfig = getEdifactExportConfig(SAMPLE_EDI_ORDERS_EXPORT, pieceIds);
    JobExecution jobExecution = testLauncher.launchStep(MAP_TO_EDIFACT_STEP, getJobParameters(exportConfig));

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    verify(ordersService).getPiecesByIdsAndReceivingStatus(pieceIds, Piece.ReceivingStatusEnum.CLAIM_SENT);
    verify(ordersService).getPoLinesByQuery(poLineQuery);
    verify(ordersService).getPurchaseOrdersByIds(anyList());
  }

  @Test
  void testEdifactClaimsExportNoPiecesFound() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(edifactExportJob);
    doReturn(List.of()).when(ordersService).getPiecesByIdsAndReceivingStatus(pieceIds, Piece.ReceivingStatusEnum.CLAIM_SENT);

    var exportConfig = getEdifactExportConfig(SAMPLE_EDI_ORDERS_EXPORT, pieceIds);
    JobExecution jobExecution = testLauncher.launchStep(MAP_TO_EDIFACT_STEP, getJobParameters(exportConfig));

    assertThat(jobExecution.getExitStatus().getExitCode()).isEqualTo(ExitStatus.FAILED.getExitCode());
    assertThat(jobExecution.getExitStatus().getExitDescription()).contains("Entities not found: Piece");
    verify(ordersService).getPiecesByIdsAndReceivingStatus(pieceIds, Piece.ReceivingStatusEnum.CLAIM_SENT);
  }

  @Test
  void testEdifactClaimsExportMissingRequiredFields() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(edifactExportJob);
    var exportConfig = getEdifactExportConfig(SAMPLE_EDI_ORDERS_EXPORT, List.of());

    JobExecution jobExecution = testLauncher.launchStep(MAP_TO_EDIFACT_STEP, getJobParameters(exportConfig));
    var status = new ArrayList<>(jobExecution.getStepExecutions()).get(0).getStatus();

    assertEquals(BatchStatus.FAILED, status);
    assertThat(jobExecution.getExitStatus().getExitCode()).isEqualTo(ExitStatus.FAILED.getExitCode());
    assertThat(jobExecution.getExitStatus().getExitDescription()).contains("Export configuration is incomplete, missing required fields: [claimPieceIds]");
  }

  @Override
  protected ObjectNode getEdifactExportConfig(String path) throws IOException {
    return getEdifactExportConfig(path, pieceIds);
  }

  protected ObjectNode getEdifactExportConfig(String path, List<String> pieceIds) throws IOException {
    var exportConfig = super.getEdifactExportConfig(path);
    exportConfig.put(INTEGRATION_TYPE.getName(), CLAIMING.getValue());
    exportConfig.put(TRANSMISSION_METHOD.getName(), FTP.getValue());
    var arr = exportConfig.putArray(CLAIM_PIECE_IDS.getName());
    pieceIds.forEach(arr::add);
    return exportConfig;
  }

}
