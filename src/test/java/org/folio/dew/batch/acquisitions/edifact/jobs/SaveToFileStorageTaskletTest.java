package org.folio.dew.batch.acquisitions.edifact.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dew.utils.Constants.EDIFACT_EXPORT_DIR_NAME;
import static org.folio.dew.utils.Constants.getWorkingDirectory;
import static org.folio.dew.utils.TestUtils.getMockData;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
import org.folio.dew.BaseBatchTest;
import org.folio.dew.batch.acquisitions.edifact.services.OrganizationsService;
import org.folio.dew.repository.FTPObjectStorageRepository;
import org.folio.dew.repository.LocalFilesStorage;
import org.folio.dew.repository.SFTPObjectStorageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
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
  @Autowired
  private LocalFilesStorage localFilesStorage;
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

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

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

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
  }

  private JobParameters getSFTPJobParameters() throws IOException {
    JobParametersBuilder paramsBuilder = new JobParametersBuilder();

    paramsBuilder.addString("edifactOrdersExport", getMockData("edifact/edifactOrdersExport.json"));
    paramsBuilder.addString("jobId", UUID.randomUUID().toString());

    String workDir = getWorkingDirectory(springApplicationName, EDIFACT_EXPORT_DIR_NAME);

    // prepare local file copy
    var filename = "testEdiFile.edi";
    var fileContent = RandomStringUtils.random(100);
    var uploadedFilePath = localFilesStorage.write(workDir + filename, fileContent.getBytes(StandardCharsets.UTF_8));
    paramsBuilder.addString("uploadedFilePath", uploadedFilePath);

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
