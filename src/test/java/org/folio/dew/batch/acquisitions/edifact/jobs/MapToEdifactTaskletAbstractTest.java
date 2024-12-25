package org.folio.dew.batch.acquisitions.edifact.jobs;

import static org.folio.dew.domain.dto.JobParameterNames.EDIFACT_ORDERS_EXPORT;
import static org.folio.dew.domain.dto.JobParameterNames.JOB_ID;
import static org.folio.dew.domain.dto.JobParameterNames.JOB_NAME;
import static org.folio.dew.utils.TestUtils.getMockData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.folio.dew.BaseBatchTest;
import org.folio.dew.batch.acquisitions.edifact.mapper.ExportResourceMapper;
import org.folio.dew.batch.acquisitions.edifact.services.OrdersService;
import org.folio.dew.client.DataExportSpringClient;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

abstract class MapToEdifactTaskletAbstractTest extends BaseBatchTest {

  protected static final String SAMPLE_PURCHASE_ORDERS_PATH = "edifact/acquisitions/purchase_order_collection.json";
  protected static final String SAMPLE_PO_LINES_PATH = "edifact/acquisitions/po_line_collection.json";
  protected static final String MAP_TO_EDIFACT_STEP = "mapToEdifactStep";

  protected static final String SAMPLE_EDI_ORDERS_EXPORT = "edifact/edifactOrdersExport.json";
  private static final String SAMPLE_EDI_ORDERS_EXPORT_MISSING_PORT = "edifact/edifactOrdersExportWithoutPort.json";

  @MockBean
  protected OrdersService ordersService;
  @MockBean
  protected DataExportSpringClient dataExportSpringClient;
  @MockBean
  @Qualifier("edifactMapper")
  protected ExportResourceMapper edifactMapper;
  @Autowired
  protected ObjectMapper objectMapper;
  protected Job edifactExportJob;

  @Test
  void testEdifactExportMissingFtpPort() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(edifactExportJob);
    JobExecution jobExecution = testLauncher.launchStep(MAP_TO_EDIFACT_STEP, getJobParameters(getEdifactExportConfig(SAMPLE_EDI_ORDERS_EXPORT_MISSING_PORT)));
    var status = new ArrayList<>(jobExecution.getStepExecutions()).get(0).getStatus();

    assertEquals(BatchStatus.FAILED, status);
    assertThat(jobExecution.getExitStatus().getExitCode()).isEqualTo(ExitStatus.FAILED.getExitCode());
    assertThat(jobExecution.getExitStatus().getExitDescription()).contains("Export configuration is incomplete, missing required fields: [ftpPort, serverAddress]");
  }

  @Test
  void testEdifactExportPurchaseOrdersNotFound() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(edifactExportJob);
    doReturn(List.of()).when(ordersService).getPoLinesByQuery(anyString());

    JobExecution jobExecution = testLauncher.launchStep(MAP_TO_EDIFACT_STEP, getJobParameters(getEdifactExportConfig(SAMPLE_EDI_ORDERS_EXPORT)));

    assertThat(jobExecution.getExitStatus().getExitCode()).isEqualTo(ExitStatus.FAILED.getExitCode());
    assertThat(jobExecution.getExitStatus().getExitDescription()).contains("Entities not found: PurchaseOrder");
    verify(ordersService).getPoLinesByQuery(anyString());
    verify(ordersService).getPurchaseOrdersByIds(anyList());
  }

  protected ObjectNode getEdifactExportConfig(String path) throws IOException {
    return (ObjectNode) objectMapper.readTree(getMockData(path));
  }

  protected JobParameters getJobParameters(ObjectNode edifactExport) {
    return new JobParametersBuilder()
      .addString(JOB_ID, UUID.randomUUID().toString())
      .addString(JOB_NAME, "000015")
      .addString(EDIFACT_ORDERS_EXPORT, edifactExport.toString())
      .toJobParameters();
  }

}
