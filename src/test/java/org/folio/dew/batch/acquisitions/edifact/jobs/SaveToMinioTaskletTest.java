package org.folio.dew.batch.acquisitions.edifact.jobs;

import static org.folio.dew.utils.TestUtils.getMockData;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

import java.io.IOException;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
import org.folio.dew.BaseBatchTest;
import org.folio.dew.batch.acquisitions.edifact.services.OrganizationsService;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

import com.fasterxml.jackson.databind.JsonNode;

class SaveToMinioTaskletTest extends BaseBatchTest {
  @Autowired
  @Qualifier("edifactOrdersExportJob")
  private Job edifactExportJob;
  @MockBean
  private OrganizationsService organizationsService;

  @Test
  @DirtiesContext
  void minioUploadSuccessful() throws IOException {
    JobLauncherTestUtils testLauncher = createTestLauncher(edifactExportJob);

    JsonNode vendorJson = objectMapper.readTree("{\"code\": \"GOBI\"}");
    doReturn(vendorJson).when(organizationsService).getOrganizationById(anyString());

    JobExecution jobExecution = testLauncher.launchStep("saveToMinIOStep", getJobParameters(), getExecutionContext());

    assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
  }


  private JobParameters getJobParameters() throws IOException {
    JobParametersBuilder paramsBuilder = new JobParametersBuilder();

    paramsBuilder.addString("edifactOrdersExport", getMockData("edifact/edifactOrdersExport.json"));
    paramsBuilder.addString("edifactOrderAsString", RandomStringUtils.random(100, true, true));
    var jobId = UUID.randomUUID().toString();
    paramsBuilder.addString("jobId", jobId);

    return paramsBuilder.toJobParameters();
  }

  private ExecutionContext getExecutionContext() {
    ExecutionContext executionContext = new ExecutionContext();
    executionContext.put("edifactOrderAsString", RandomStringUtils.random(100, true, true));
    return executionContext;
  }

  protected JobLauncherTestUtils createTestLauncher(Job job) {
    JobLauncherTestUtils testLauncher = new JobLauncherTestUtils();
    testLauncher.setJob(job);
    testLauncher.setJobLauncher(jobLauncher);
    testLauncher.setJobRepository(jobRepository);
    return testLauncher;
  }
}
