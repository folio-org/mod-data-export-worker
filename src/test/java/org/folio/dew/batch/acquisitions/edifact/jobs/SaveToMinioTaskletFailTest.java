package org.folio.dew.batch.acquisitions.edifact.jobs;

import static org.folio.dew.utils.TestUtils.getMockData;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import java.io.IOException;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
import org.folio.dew.BaseBatchTest;
import org.folio.dew.batch.acquisitions.edifact.services.OrganizationsService;
import org.folio.dew.repository.RemoteFilesStorage;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.JsonNode;

class SaveToMinioTaskletFailTest extends BaseBatchTest {
  @Autowired
  private Job edifactExportJob;
  @MockBean
  private OrganizationsService organizationsService;

  @MockBean
  private RemoteFilesStorage remoteFilesStorage;

  private static final String NULL_POINTER_ERROR_TEXT = "Test null pointer exception message";

  @Test
  @DirtiesContext
  void minioSaveToRemoteStorageFailed()
      throws IOException {
    JobLauncherTestUtils testLauncher = createTestLauncher(edifactExportJob);

    JsonNode vendorJson = objectMapper.readTree("{\"code\": \"GOBI\"}");
    doReturn(vendorJson).when(organizationsService).getOrganizationById(anyString());

    doThrow(new NullPointerException(NULL_POINTER_ERROR_TEXT)).when(remoteFilesStorage).write(anyString(), any());

    JobExecution jobExecution = testLauncher.launchStep("saveToMinIOStep", getJobParameters());

    assertEquals(ExitStatus.FAILED.getExitCode(), jobExecution.getExitStatus().getExitCode());
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