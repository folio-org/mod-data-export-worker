package org.folio.dew.batch.acquisitions.edifact.jobs;

import static org.folio.dew.utils.TestUtils.getMockData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import org.folio.dew.BaseBatchTest;
import org.folio.dew.batch.acquisitions.edifact.PurchaseOrdersToEdifactMapper;
import org.folio.dew.batch.acquisitions.edifact.services.OrdersService;
import org.folio.dew.domain.dto.CompositePurchaseOrder;
import org.folio.dew.domain.dto.PurchaseOrderCollection;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
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
    PurchaseOrderCollection poCollection = objectMapper.readValue(getMockData("edifact/acquisitions/purchase_order_collection.json"), PurchaseOrderCollection.class);
    CompositePurchaseOrder comPO = objectMapper.readValue(getMockData("edifact/acquisitions/composite_purchase_order.json"), CompositePurchaseOrder.class);

    doReturn(poCollection).when(ordersService).getCompositePurchaseOrderByQuery(anyString(), anyInt());
    doReturn(comPO).when(ordersService).getCompositePurchaseOrderById(anyString());
    doReturn("test1").when(purchaseOrdersToEdifactMapper).convertOrdersToEdifact(any(), any());

    JobExecution jobExecution = testLauncher.launchStep("mapToEdifactStep", getJobParameters());
    Collection<StepExecution> actualStepExecutions = jobExecution.getStepExecutions();

    var status = new ArrayList<>(actualStepExecutions).get(0).getStatus().getBatchStatus().name();
    assertEquals("COMPLETED", status);

  }

  @Test
  void purchaseOrdersNotFound() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(edifactExportJob);
    PurchaseOrderCollection poCollection = new PurchaseOrderCollection();
    doReturn(poCollection).when(ordersService).getCompositePurchaseOrderByQuery(anyString(), anyInt());
    // when
    JobExecution jobExecution = testLauncher.launchStep("mapToEdifactStep", getJobParameters());

    // then
    assertThat(jobExecution.getExitStatus().getExitDescription(), containsString("Orders for export not found"));
  }

  private JobParameters getJobParameters() throws IOException {
    JobParametersBuilder paramsBuilder = new JobParametersBuilder();
    JSONObject edifactOrdersExportJson = new JSONObject(getMockData("edifact/edifactOrdersExport.json"));

    paramsBuilder.addString("jobId", UUID.randomUUID().toString());
    paramsBuilder.addString("edifactOrdersExport", edifactOrdersExportJson.toString());

    return paramsBuilder.toJobParameters();
  }
}
