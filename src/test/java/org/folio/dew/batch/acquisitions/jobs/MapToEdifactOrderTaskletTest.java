package org.folio.dew.batch.acquisitions.jobs;

import static org.assertj.core.api.Assertions.assertThat;
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

import org.assertj.core.api.Assertions;
import org.folio.dew.domain.dto.ExportConfigCollection;
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

class MapToEdifactOrderTaskletTest extends MapToEdifactTaskletAbstractTest {

  private static final String DATA_EXPORT_CONFIGS_PATH = "edifact/dataExportConfigs.json";
  private static final String SAMPLE_EDI_ORDERS_EXPORT_MISSING_FIELDS = "edifact/edifactOrdersExportWithoutRequiredFields.json";

  @Autowired
  Job edifactOrdersExportJob;

  private List<PurchaseOrder> orders;
  private List<PoLine> poLines;

  @BeforeEach
  @SneakyThrows
  @Override
  protected void setUp() {
    super.setUp();
    edifactExportJob = edifactOrdersExportJob;
    orders = objectMapper.readValue(getMockData(SAMPLE_PURCHASE_ORDERS_PATH), PurchaseOrderCollection.class).getPurchaseOrders();
    poLines = objectMapper.readValue(getMockData(SAMPLE_PO_LINES_PATH), PoLineCollection.class).getPoLines();

    doReturn(objectMapper.readTree("{\"code\": \"GOBI\"}")).when(organizationsService).getOrganizationById(anyString());
  }

  @Test
  void testEdifactOrdersExport() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(edifactExportJob);
    String cqlString = "(purchaseOrder.workflowStatus==Open)" +
      " AND (purchaseOrder.vendor==d0fb5aa0-cdf1-11e8-a8d5-f2801f1b9fd1)" +
      " AND (cql.allRecords=1 NOT purchaseOrder.manualPo==true)" +
      " AND (automaticExport==true)" +
      " AND (cql.allRecords=1 NOT lastEDIExportDate=\"\")" +
      " AND (acquisitionMethod==(\"306489dd-0053-49ee-a068-c316444a8f55\"))" +
      " AND (vendorDetail.vendorAccount==(\"BRXXXXX-01\"))";
    doReturn(poLines).when(ordersService).getPoLinesByQuery(cqlString);
    doReturn(orders).when(ordersService).getPurchaseOrdersByIds(anyList());
    doReturn("test1").when(edifactMapper).convertForExport(any(), any(), any(), anyString());

    JobExecution jobExecution = testLauncher.launchStep(MAP_TO_EDIFACT_STEP, getJobParameters(getEdifactExportConfig(SAMPLE_EDI_ORDERS_EXPORT)));

    Assertions.assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    verify(ordersService).getPoLinesByQuery(cqlString);
    verify(ordersService).getPurchaseOrdersByIds(anyList());
  }

  @Test
  void testEdifactOrdersExportDefaultConfig() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(edifactExportJob);
    String cqlString = "(purchaseOrder.workflowStatus==Open)" +
      " AND (purchaseOrder.vendor==d0fb5aa0-cdf1-11e8-a8d5-f2801f1b9fd1)" +
      " AND (cql.allRecords=1 NOT purchaseOrder.manualPo==true)" +
      " AND (automaticExport==true)" +
      " AND (cql.allRecords=1 NOT lastEDIExportDate=\"\")" +
      " AND (acquisitionMethod==(\"306489dd-0053-49ee-a068-c316444a8f55\"))";
    String configSql = "configName==EDIFACT_ORDERS_EXPORT_d0fb5aa0-cdf1-11e8-a8d5-f2801f1b9fd1*";
    ExportConfigCollection exportConfigCollection = new ExportConfigCollection();
    exportConfigCollection.setTotalRecords(1);
    poLines.get(0).getVendorDetail().setVendorAccount(null);
    doReturn(poLines).when(ordersService).getPoLinesByQuery(cqlString);
    doReturn(exportConfigCollection).when(dataExportSpringClient).getExportConfigs(configSql);
    doReturn(orders).when(ordersService).getPurchaseOrdersByIds(anyList());
    doReturn("test1").when(edifactMapper).convertForExport(any(), any(), any(), anyString());

    var exportConfig = getEdifactExportConfig(SAMPLE_EDI_ORDERS_EXPORT, true);
    JobExecution jobExecution = testLauncher.launchStep(MAP_TO_EDIFACT_STEP, getJobParameters(exportConfig));

    Assertions.assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    verify(ordersService).getPoLinesByQuery(cqlString);
    verify(ordersService).getPurchaseOrdersByIds(anyList());
  }

  @Test
  void testEdifactOrdersExportDefaultConfigWithTwoExportConfigs() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(edifactExportJob);
    String cqlString = "(purchaseOrder.workflowStatus==Open)" +
      " AND (purchaseOrder.vendor==d0fb5aa0-cdf1-11e8-a8d5-f2801f1b9fd1)" +
      " AND (cql.allRecords=1 NOT purchaseOrder.manualPo==true)" +
      " AND (automaticExport==true)" +
      " AND (cql.allRecords=1 NOT lastEDIExportDate=\"\")" +
      " AND (acquisitionMethod==(\"306489dd-0053-49ee-a068-c316444a8f55\"))" +
      " AND (cql.allRecords=1 NOT vendorDetail.vendorAccount==(\"org1\" or \"org2\"))";
    String configSql = "configName==EDIFACT_ORDERS_EXPORT_d0fb5aa0-cdf1-11e8-a8d5-f2801f1b9fd1*";
    ExportConfigCollection exportConfigCollection = objectMapper.readValue(getMockData(DATA_EXPORT_CONFIGS_PATH), ExportConfigCollection.class);
    poLines.get(0).getVendorDetail().setVendorAccount(null);
    doReturn(poLines).when(ordersService).getPoLinesByQuery(cqlString);
    doReturn(exportConfigCollection).when(dataExportSpringClient).getExportConfigs(configSql);
    doReturn(orders).when(ordersService).getPurchaseOrdersByIds(anyList());
    doReturn("test1").when(edifactMapper).convertForExport(any(), any(), any(), anyString());

    var exportConfig = getEdifactExportConfig(SAMPLE_EDI_ORDERS_EXPORT, true);
    JobExecution jobExecution = testLauncher.launchStep(MAP_TO_EDIFACT_STEP, getJobParameters(exportConfig));

    Assertions.assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    verify(ordersService).getPoLinesByQuery(cqlString);
    verify(ordersService).getPurchaseOrdersByIds(anyList());
  }

  @Test
  void testEdifactOrdersExportMissingRequiredFields() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(edifactExportJob);
    JobExecution jobExecution = testLauncher.launchStep(MAP_TO_EDIFACT_STEP, getJobParameters(getEdifactExportConfig(SAMPLE_EDI_ORDERS_EXPORT_MISSING_FIELDS)));
    var status = new ArrayList<>(jobExecution.getStepExecutions()).get(0).getStatus();

    assertEquals(BatchStatus.FAILED, status);
    assertThat(jobExecution.getExitStatus().getExitCode()).isEqualTo(ExitStatus.FAILED.getExitCode());
    assertThat(jobExecution.getExitStatus().getExitDescription()).contains("Export configuration is incomplete, missing required fields: [libEdiCode, vendorEdiType]");
  }

  protected ObjectNode getEdifactExportConfig(String path, boolean isDefaultConfig) throws IOException {
    return getEdifactExportConfig(path).put("isDefaultConfig", isDefaultConfig);
  }

}
