package org.folio.dew.batch.acquisitions.edifact.jobs;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.RandomStringUtils;
import org.folio.dew.BaseBatchTest;
import org.folio.dew.batch.acquisitions.edifact.services.OrganizationsService;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.folio.dew.domain.dto.JobParameterNames.EDIFACT_FILE_NAME;
import static org.folio.dew.utils.TestUtils.getMockData;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

class ExportHistoryTaskletTest extends BaseBatchTest {
  @Autowired
  private Job edifactExportJob;
  @MockBean
  private OrganizationsService organizationsService;

  @Test
  @DirtiesContext
  void createExportHistoryFailedWhenFileNotFound() throws IOException {
    JobLauncherTestUtils testLauncher = createTestLauncher(edifactExportJob);
    Map map = new HashMap();
    map.put(EDIFACT_FILE_NAME,"TestFile");
    JsonNode vendorJson = objectMapper.readTree("{\"code\": \"GOBI\"}");
    doReturn(vendorJson).when(organizationsService).getOrganizationById(anyString());
    JobExecution jobExecution = testLauncher.launchStep("createExportHistoryRecordsStep", getJobParameters());

    var status = new ArrayList<>(jobExecution.getStepExecutions()).get(0)
        .getStatus()
        .name();
    assertEquals("FAILED", status);
  }

  private JobParameters getJobParameters() throws IOException {
    JobParametersBuilder paramsBuilder = new JobParametersBuilder();
    paramsBuilder.addString("edifactOrdersExport", getMockData("edifact/edifactOrdersExport.json"));
    paramsBuilder.addString("edifactOrderAsString", RandomStringUtils.random(100, true, true));
    var jobId = UUID.randomUUID().toString();
    paramsBuilder.addString("jobId", jobId);

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
