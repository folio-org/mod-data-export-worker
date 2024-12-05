package org.folio.dew.batch.acquisitions.edifact.jobs;

import static org.folio.dew.utils.TestUtils.getMockData;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.folio.dew.domain.dto.ExportConfigCollection;
import org.folio.dew.domain.dto.PoLine;
import org.folio.dew.domain.dto.PoLineCollection;
import org.folio.dew.domain.dto.PurchaseOrder;
import org.folio.dew.domain.dto.PurchaseOrderCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;

class MapToEdifactOrderTaskletTest extends MapToEdifactTaskletAbstractTest {

  @Autowired
  Job edifactOrdersExportJob;

  @BeforeEach
  @Override
  protected void setUp() {
    super.setUp();
    edifactExportJob = edifactOrdersExportJob;
  }

  @Test
  void edifactOrdersExportJobTestSuccess() throws Exception {
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
  void edifactOrdersExportJobIfDefaultConfigTestSuccess() throws Exception {
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
      " AND acquisitionMethod==(\"306489dd-0053-49ee-a068-c316444a8f55\")";
    String configSql = "configName==EDIFACT_ORDERS_EXPORT_d0fb5aa0-cdf1-11e8-a8d5-f2801f1b9fd1*";
    ExportConfigCollection exportConfigCollection = new ExportConfigCollection();
    exportConfigCollection.setTotalRecords(1);
    poLines.get(0).getVendorDetail().setVendorAccount(null);
    doReturn(poLines).when(ordersService).getPoLinesByQuery(cqlString);
    doReturn(exportConfigCollection).when(dataExportSpringClient).getExportConfigs(configSql);
    doReturn(orders).when(ordersService).getPurchaseOrdersByIds(anyList());
    doReturn("test1").when(purchaseOrdersToEdifactMapper).convertOrdersToEdifact(any(), any(), anyString());

    JobExecution jobExecution = testLauncher.launchStep("mapToEdifactStep", getJobParameters(true));

    Assertions.assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    verify(ordersService).getPoLinesByQuery(cqlString);
    verify(ordersService).getPurchaseOrdersByIds(anyList());
  }

  @Test
  void edifactOrdersExportJobIfDefaultConfigNotOneTestSuccess() throws Exception {
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
      " AND cql.allRecords=1 NOT vendorDetail.vendorAccount==(\"org1\" OR \"org2\")";
    String configSql = "configName==EDIFACT_ORDERS_EXPORT_d0fb5aa0-cdf1-11e8-a8d5-f2801f1b9fd1*";
    ExportConfigCollection exportConfigCollection = objectMapper.readValue(getMockData("edifact/dataExportConfigs.json"), ExportConfigCollection.class);
    poLines.get(0).getVendorDetail().setVendorAccount(null);
    doReturn(poLines).when(ordersService).getPoLinesByQuery(cqlString);
    doReturn(exportConfigCollection).when(dataExportSpringClient).getExportConfigs(configSql);
    doReturn(orders).when(ordersService).getPurchaseOrdersByIds(anyList());
    doReturn("test1").when(purchaseOrdersToEdifactMapper).convertOrdersToEdifact(any(), any(), anyString());

    JobExecution jobExecution = testLauncher.launchStep("mapToEdifactStep", getJobParameters(true));

    Assertions.assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    verify(ordersService).getPoLinesByQuery(cqlString);
    verify(ordersService).getPurchaseOrdersByIds(anyList());
  }

}
