package org.folio.dew.batch.acquisitions.edifact.jobs;

import static org.folio.dew.utils.TestUtils.getMockData;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import org.folio.dew.BaseBatchTest;
import org.folio.dew.batch.acquisitions.edifact.services.OrganizationsService;
import org.folio.dew.repository.FTPObjectStorageRepository;
import org.folio.dew.repository.SFTPObjectStorageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

import com.fasterxml.jackson.databind.JsonNode;

class SaveToFileStorageTaskletTest extends BaseBatchTest {
  @Autowired
  private Job edifactExportJob;
  @MockBean
  private SFTPObjectStorageRepository sftpObjectStorageRepository;
  @MockBean
  private FTPObjectStorageRepository ftpObjectStorageRepository;
  @MockBean
  private OrganizationsService organizationsService;

  @Test
  @DirtiesContext
  void sftpUploadSuccessful() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(edifactExportJob);

    doReturn(true).when(sftpObjectStorageRepository).upload(anyString(), anyString(), anyString(), anyInt(), anyString(), anyString(), anyString());
    JsonNode vendorJson = objectMapper.readTree("{\"code\": \"GOBI\"}");
    doReturn(vendorJson).when(organizationsService).getOrganizationById(anyString());

    JobExecution jobExecution = testLauncher.launchStep("saveToFTPStep", getSFTPJobParameters());

    var status = new ArrayList<>(jobExecution.getStepExecutions()).get(0)
      .getStatus()
      .getBatchStatus()
      .name();
    assertEquals("COMPLETED", status);
  }

  @Test
  @DirtiesContext
  void ftpUploadSuccessful() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(edifactExportJob);

    JsonNode vendorJson = objectMapper.readTree("{\"code\": \"GOBI\"}");
    doReturn(vendorJson).when(organizationsService).getOrganizationById(anyString());
    doReturn(true).when(ftpObjectStorageRepository).login(anyString(),anyString(),anyString());
    doNothing().when(ftpObjectStorageRepository).upload(anyString(), anyString());

    JobExecution jobExecution = testLauncher.launchStep("saveToFTPStep", getFTPJobParameters());

    var status = new ArrayList<>(jobExecution.getStepExecutions()).get(0)
      .getStatus()
      .getBatchStatus()
      .name();
    assertEquals("COMPLETED", status);
  }

  private JobParameters getSFTPJobParameters() throws IOException {
    JobParametersBuilder paramsBuilder = new JobParametersBuilder();

    paramsBuilder.addString("edifactOrdersExport", getMockData("edifact/edifactOrdersExport.json"));
    paramsBuilder.addString("jobId", UUID.randomUUID().toString());

    return paramsBuilder.toJobParameters();
  }

  private JobParameters getFTPJobParameters() throws IOException {
    JobParametersBuilder paramsBuilder = new JobParametersBuilder();

    paramsBuilder.addString("edifactOrdersExport", getMockData("edifact/edifactFTPOrdersExport.json"));
    paramsBuilder.addString("jobId", UUID.randomUUID().toString());

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
