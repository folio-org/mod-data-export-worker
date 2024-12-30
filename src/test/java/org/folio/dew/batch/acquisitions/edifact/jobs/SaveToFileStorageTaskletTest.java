package org.folio.dew.batch.acquisitions.edifact.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dew.domain.dto.JobParameterNames.ACQ_EXPORT_FILE;
import static org.folio.dew.domain.dto.JobParameterNames.ACQ_EXPORT_FILE_NAME;
import static org.folio.dew.domain.dto.JobParameterNames.EDIFACT_ORDERS_EXPORT;
import static org.folio.dew.domain.dto.JobParameterNames.JOB_ID;
import static org.folio.dew.utils.TestUtils.getMockData;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

import java.io.IOException;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
import org.folio.dew.BaseBatchTest;
import org.folio.dew.batch.acquisitions.edifact.services.OrganizationsService;
import org.folio.dew.repository.FTPObjectStorageRepository;
import org.folio.dew.repository.SFTPObjectStorageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

import lombok.SneakyThrows;

class SaveToFileStorageTaskletTest extends BaseBatchTest {

  @Autowired
  @Qualifier("edifactOrdersExportJob")
  private Job edifactExportJob;
  @MockBean
  private SFTPObjectStorageRepository sftpObjectStorageRepository;
  @MockBean
  private FTPObjectStorageRepository ftpObjectStorageRepository;
  @MockBean
  private OrganizationsService organizationsService;

  @Override
  @SneakyThrows
  @BeforeEach
  protected void setUp() {
    super.setUp();

    doReturn(objectMapper.readTree("{\"code\": \"GOBI\"}")).when(organizationsService).getOrganizationById(anyString());
    doReturn(true).when(sftpObjectStorageRepository).upload(anyString(), anyString(), anyString(), anyInt(), anyString(), anyString(), any());
    doNothing().when(ftpObjectStorageRepository).upload(anyString(), anyString(), anyString(), anyString(), anyString(), any());
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource(value = {"SFTP Upload,edifact/edifactOrdersExport.json", "FTP Upload,edifact/edifactFTPOrdersExport.json"}, delimiter = ',')
  @DirtiesContext
  void testUploadSuccessful(String testName, String edifactOrdersExport) throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(edifactExportJob);

    var jobParameters = getJobParameters(edifactOrdersExport);
    ExecutionContext executionContext = getExecutionContext();
    JobExecution jobExecution = testLauncher.launchStep("saveToFTPStep", jobParameters, executionContext);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

  }

  private JobParameters getJobParameters(String edifactOrdersExport) throws IOException {
    JobParametersBuilder paramsBuilder = new JobParametersBuilder();
    paramsBuilder.addString(EDIFACT_ORDERS_EXPORT, getMockData(edifactOrdersExport));
    paramsBuilder.addString(JOB_ID, UUID.randomUUID().toString());
    return paramsBuilder.toJobParameters();
  }

  private ExecutionContext getExecutionContext() {
    // Prepare file name and content
    ExecutionContext executionContext = new ExecutionContext();
    executionContext.put(ACQ_EXPORT_FILE_NAME, "testEdiFile.edi");
    executionContext.put(ACQ_EXPORT_FILE, RandomStringUtils.random(100));
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
