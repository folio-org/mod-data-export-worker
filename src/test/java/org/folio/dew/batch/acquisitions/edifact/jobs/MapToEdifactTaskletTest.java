package org.folio.dew.batch.acquisitions.edifact.jobs;

import static org.folio.dew.utils.TestUtils.getMockData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.assertj.core.api.Assertions;
import org.folio.dew.BaseBatchTest;
import org.folio.dew.batch.acquisitions.edifact.PurchaseOrdersToEdifactMapper;
import org.folio.dew.batch.acquisitions.edifact.services.OrdersService;
import org.folio.dew.domain.dto.PoLine;
import org.folio.dew.domain.dto.PoLineCollection;
import org.folio.dew.domain.dto.PurchaseOrder;
import org.folio.dew.domain.dto.PurchaseOrderCollection;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.fasterxml.jackson.databind.ObjectMapper;

class MapToEdifactTaskletTest extends BaseBatchTest {

  @MockBean
  private OrdersService ordersService;
  @MockBean
  private PurchaseOrdersToEdifactMapper purchaseOrdersToEdifactMapper;

  @Autowired
  protected ObjectMapper objectMapper;
  @Autowired
  Job edifactExportJob;

  @Test
  void edifactExportJobTestSuccess() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(edifactExportJob);
    List<PurchaseOrder> orders = objectMapper.readValue(getMockData(
      "edifact/acquisitions/purchase_order_collection.json"), PurchaseOrderCollection.class).getPurchaseOrders();
    List<PoLine> poLines = objectMapper.readValue(getMockData("edifact/acquisitions/po_line_collection.json"),
      PoLineCollection.class).getPoLines();
    String cqlString = "purchaseOrder.workflowStatus==Open" +
      " AND purchaseOrder.vendor==d0fb5aa0-cdf1-11e8-a8d5-f2801f1b9fd1" +
      " AND (cql.allRecords=1 NOT purchaseOrder.manualPo==true)" +
      " AND automaticExport==true" +
      " AND (cql.allRecords=1 NOT lastEDIExportDate=\"\")" +
      " AND acquisitionMethod==(\"306489dd-0053-49ee-a068-c316444a8f55\")" +
      " AND vendorDetail.vendorAccount==(\"BRXXXXX-01\")";
    doReturn(poLines).when(ordersService).getPoLinesByQuery(cqlString);
    doReturn(orders).when(ordersService).getPurchaseOrdersByIds(anyList());
    doReturn("test1").when(purchaseOrdersToEdifactMapper).convertOrdersToEdifact(any(), any(), anyString());

    JobExecution jobExecution = testLauncher.launchStep("mapToEdifactStep", getJobParameters(false));

    Assertions.assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    verify(ordersService).getPoLinesByQuery(cqlString);
    verify(ordersService).getPurchaseOrdersByIds(anyList());
  }

  @Test
  void testShouldReturnEdifactExceptionBecauseRequiredFieldsIsNull() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(edifactExportJob);
    JobExecution jobExecution = testLauncher.launchStep("mapToEdifactStep", getJobParametersWithoutRequiredFields());
    String expectedMessage = "Export configuration is incomplete, missing library EDI code/Vendor EDI code";
    var status = new ArrayList<>(jobExecution.getStepExecutions()).get(0).getStatus().name();

    assertTrue(jobExecution.getExitStatus().getExitDescription().contains(expectedMessage));
    assertEquals("FAILED", status);
  }

  @Test
  void testShouldReturnEdifactExceptionBecauseFtpPortIsNull() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(edifactExportJob);
    JobExecution jobExecution = testLauncher.launchStep("mapToEdifactStep", getJobParametersWithoutPort());
    String expectedMessage = "Export configuration is incomplete, missing FTP/SFTP Port";
    var status = new ArrayList<>(jobExecution.getStepExecutions()).get(0).getStatus().name();

    assertTrue(jobExecution.getExitStatus().getExitDescription().contains(expectedMessage));
    assertEquals("FAILED", status);
  }

  @Test
  void edifactExportJobIfDefaultConfigTestSuccess() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(edifactExportJob);
    List<PurchaseOrder> orders = objectMapper.readValue(getMockData(
      "edifact/acquisitions/purchase_order_collection.json"), PurchaseOrderCollection.class).getPurchaseOrders();
    List<PoLine> poLines = objectMapper.readValue(getMockData("edifact/acquisitions/po_line_collection.json"),
      PoLineCollection.class).getPoLines();
    String cqlString = "purchaseOrder.workflowStatus==Open" +
      " AND purchaseOrder.vendor==d0fb5aa0-cdf1-11e8-a8d5-f2801f1b9fd1" +
      " AND (cql.allRecords=1 NOT purchaseOrder.manualPo==true)" +
      " AND automaticExport==true" +
      " AND (cql.allRecords=1 NOT lastEDIExportDate=\"\")" +
      " AND acquisitionMethod==(\"306489dd-0053-49ee-a068-c316444a8f55\")" +
      " AND (vendorDetail.vendorAccount==\"\" OR (cql.allRecords=1 NOT vendorDetail.vendorAccount=\"\"))";
    poLines.get(0).getVendorDetail().setVendorAccount(null);
    doReturn(poLines).when(ordersService).getPoLinesByQuery(cqlString);
    doReturn(orders).when(ordersService).getPurchaseOrdersByIds(anyList());
    doReturn("test1").when(purchaseOrdersToEdifactMapper).convertOrdersToEdifact(any(), any(), anyString());

    JobExecution jobExecution = testLauncher.launchStep("mapToEdifactStep", getJobParameters(true));

    Assertions.assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    verify(ordersService).getPoLinesByQuery(cqlString);
    verify(ordersService).getPurchaseOrdersByIds(anyList());
  }

  @Test
  void purchaseOrdersNotFound() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(edifactExportJob);
    List<PoLine> poLines = List.of();
    String cqlString = "purchaseOrder.workflowStatus==Open" +
      " AND purchaseOrder.vendor==d0fb5aa0-cdf1-11e8-a8d5-f2801f1b9fd1" +
      " AND (cql.allRecords=1 NOT purchaseOrder.manualPo==true)" +
      " AND automaticExport==true" +
      " AND (cql.allRecords=1 NOT lastEDIExportDate=\"\")" +
      " AND acquisitionMethod==(\"306489dd-0053-49ee-a068-c316444a8f55\")" +
      " AND vendorDetail.vendorAccount==(\"BRXXXXX-01\")";
    doReturn(poLines).when(ordersService).getPoLinesByQuery(cqlString);

    JobExecution jobExecution = testLauncher.launchStep("mapToEdifactStep", getJobParameters(false));

    assertThat(jobExecution.getExitStatus().getExitDescription(), containsString("Orders for export not found"));
    verify(ordersService).getPoLinesByQuery(cqlString);
    verify(ordersService).getPurchaseOrdersByIds(anyList());
  }

  private JobParameters getJobParameters(boolean isDefaultConfig) throws IOException {
    JobParametersBuilder paramsBuilder = new JobParametersBuilder();
    var edifactOrdersExportJson = (ObjectNode) objectMapper.readTree(getMockData("edifact/edifactOrdersExport.json"));
    edifactOrdersExportJson.put("isDefaultConfig", isDefaultConfig);

    paramsBuilder.addString("jobId", UUID.randomUUID().toString());
    paramsBuilder.addString("edifactOrdersExport", edifactOrdersExportJson.toString());
    paramsBuilder.addString("jobName", "000015");

    return paramsBuilder.toJobParameters();
  }

  private JobParameters getJobParametersWithoutRequiredFields() throws IOException {
    JobParametersBuilder paramsBuilder = new JobParametersBuilder();
    var edifactOrdersExportJson = (ObjectNode) objectMapper.readTree(getMockData("edifact/edifactOrdersExportWithoutRequiredFields.json"));
    edifactOrdersExportJson.put("isDefaultConfig", false);

    paramsBuilder.addString("jobId", UUID.randomUUID().toString());
    paramsBuilder.addString("edifactOrdersExport", edifactOrdersExportJson.toString());
    paramsBuilder.addString("jobName", "000015");

    return paramsBuilder.toJobParameters();
  }

  private JobParameters getJobParametersWithoutPort() throws IOException {
    JobParametersBuilder paramsBuilder = new JobParametersBuilder();

    paramsBuilder.addString("edifactOrdersExport", getMockData("edifact/edifactOrdersExportWithoutPort.json"));
    paramsBuilder.addString("jobId", UUID.randomUUID().toString());

    return paramsBuilder.toJobParameters();
  }
}
