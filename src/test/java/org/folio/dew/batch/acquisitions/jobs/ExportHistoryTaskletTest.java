package org.folio.dew.batch.acquisitions.jobs;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.RandomStringUtils;
import org.folio.dew.BaseBatchTest;
import org.folio.dew.batch.acquisitions.services.OrganizationsService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import static org.folio.dew.domain.dto.JobParameterNames.ACQ_EXPORT_FILE;
import static org.folio.dew.domain.dto.JobParameterNames.ACQ_EXPORT_FILE_NAME;
import static org.folio.dew.domain.dto.JobParameterNames.EDIFACT_ORDERS_EXPORT;
import static org.folio.dew.domain.dto.JobParameterNames.JOB_ID;
import static org.folio.dew.domain.dto.JobParameterNames.JOB_NAME;
import static org.folio.dew.utils.TestUtils.getMockData;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

class ExportHistoryTaskletTest extends BaseBatchTest {
  @Autowired
  @Qualifier("edifactOrdersExportJob")
  private Job edifactExportJob;
  @MockitoBean
  private OrganizationsService organizationsService;

  @BeforeAll
  static void beforeAll() {
    setUpTenant(NON_CONSORTIUM_TENANT);
  }

  @Test
  @DirtiesContext
  void testCreateExportHistoryFailedWhenFileNotFound() throws IOException {
    JobLauncherTestUtils testLauncher = createTestLauncher(edifactExportJob);
    JsonNode vendorJson = objectMapper.readTree("{\"code\": \"GOBI\"}");
    doReturn(vendorJson).when(organizationsService).getOrganizationById(anyString());

    JobExecution jobExecution = testLauncher.launchStep("createExportHistoryRecordsStep", getJobParameters());

    var status = new ArrayList<>(jobExecution.getStepExecutions()).get(0)
      .getStatus()
      .name();
    assertEquals("FAILED", status);
  }

  @Test
  @DirtiesContext
  void testCreateExportHistoryWithCompleteStatus() throws IOException {
    JobLauncherTestUtils testLauncher = createTestLauncher(edifactExportJob);
    JsonNode vendorJson = objectMapper.readTree("{\"code\": \"GOBI\"}");
    doReturn(vendorJson).when(organizationsService).getOrganizationById(anyString());

    JobExecution jobExecution1 = testLauncher.launchStep("createExportHistoryRecordsStep", getJobParameters(), getExecutionContext());

    var status = new ArrayList<>(jobExecution1.getStepExecutions()).get(0)
      .getStatus()
      .name();
    assertEquals("COMPLETED", status);
  }

  protected ExecutionContext getExecutionContext() {
    ExecutionContext result = new ExecutionContext();
    result.put(ACQ_EXPORT_FILE_NAME, "test_file");
    return result;
  }

  private JobParameters getJobParameters() throws IOException {
    JobParametersBuilder paramsBuilder = new JobParametersBuilder();

    paramsBuilder.addString(EDIFACT_ORDERS_EXPORT, getMockData("edifact/edifactOrdersExport.json"));
    paramsBuilder.addString(ACQ_EXPORT_FILE, RandomStringUtils.secure().next(100, true, true));
    paramsBuilder.addString(JOB_NAME, "TestJob00123");
    paramsBuilder.addString(JOB_ID, UUID.randomUUID().toString());

    return paramsBuilder.toJobParameters();
  }

  protected JobLauncherTestUtils createTestLauncher(Job job) {
    JobLauncherTestUtils testLauncher = new JobLauncherTestUtils();
    testLauncher.setJob(job);
    testLauncher.setJobLauncher(jobLauncher);
    testLauncher.setJobRepository(jobRepository);
    return testLauncher;
  }
}
