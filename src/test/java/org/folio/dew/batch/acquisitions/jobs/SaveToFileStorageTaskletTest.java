package org.folio.dew.batch.acquisitions.jobs;

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
import org.folio.dew.batch.acquisitions.services.OrganizationsService;
import org.folio.dew.domain.dto.acquisitions.edifact.Organization;
import org.folio.dew.repository.FTPObjectStorageRepository;
import org.folio.dew.repository.SFTPObjectStorageRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.test.JobOperatorTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;

import lombok.SneakyThrows;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class SaveToFileStorageTaskletTest extends BaseBatchTest {

  @Autowired
  @Qualifier("edifactOrdersExportJob")
  private Job edifactExportJob;
  @MockitoBean
  private SFTPObjectStorageRepository sftpObjectStorageRepository;
  @MockitoBean
  private FTPObjectStorageRepository ftpObjectStorageRepository;
  @MockitoBean
  private OrganizationsService organizationsService;

  @BeforeAll
  static void beforeAll() {
    setUpTenant(NON_CONSORTIUM_TENANT);
  }

  @Override
  @SneakyThrows
  @BeforeEach
  protected void setUp() {
    super.setUp();

    Organization organization = new Organization();
    organization.setCode("GOBI");
    doReturn(organization).when(organizationsService).getOrganizationById(anyString());
    doReturn(true).when(sftpObjectStorageRepository).upload(anyString(), anyString(), anyString(), anyInt(), anyString(), anyString(), any());
    doNothing().when(ftpObjectStorageRepository).upload(anyString(), anyString(), anyString(), anyString(), anyString(), any());
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource(value = {"SFTP Upload,edifact/edifactOrdersExport.json", "FTP Upload,edifact/edifactFTPOrdersExport.json"}, delimiter = ',')
  @DirtiesContext
  void testUploadSuccessful(String testName, String edifactOrdersExport) throws Exception {
    JobOperatorTestUtils testLauncher = createTestLauncher(edifactExportJob);

    var jobParameters = getJobParameters(edifactOrdersExport);
    ExecutionContext executionContext = getExecutionContext();
    JobExecution jobExecution = testLauncher.startStep("saveToFTPStep", jobParameters, executionContext);

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
    executionContext.put(ACQ_EXPORT_FILE, RandomStringUtils.secure().next(100));
    return executionContext;
  }

}
