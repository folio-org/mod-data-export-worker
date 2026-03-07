package org.folio.dew.batch.acquisitions.jobs;

import static org.folio.dew.domain.dto.JobParameterNames.ACQ_EXPORT_FILE;
import static org.folio.dew.domain.dto.JobParameterNames.EDIFACT_ORDERS_EXPORT;
import static org.folio.dew.domain.dto.JobParameterNames.JOB_ID;
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
import org.folio.dew.batch.acquisitions.services.OrganizationsService;
import org.folio.dew.domain.dto.acquisitions.edifact.Organization;
import org.folio.dew.repository.RemoteFilesStorage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import com.fasterxml.jackson.databind.JsonNode;

import lombok.SneakyThrows;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

class SaveToMinioTaskletTest extends BaseBatchTest {
  @Autowired
  @Qualifier("edifactOrdersExportJob")
  private Job edifactExportJob;
  @MockitoBean
  private OrganizationsService organizationsService;
  @MockitoSpyBean
  private RemoteFilesStorage remoteFilesStorage;


  @BeforeAll
  static void beforeAll() {
    setUpTenant(NON_CONSORTIUM_TENANT);
  }

  @Override
  @SneakyThrows
  @BeforeEach
  protected void setUp() {
    super.setUp();

    var vendor = new Organization();
    vendor.setCode("GOBI");
    doReturn(vendor).when(organizationsService).getOrganizationById(anyString());
  }

  @Test
  @DirtiesContext
  void minioUploadSuccessful() throws IOException {
    JobOperatorTestUtils testLauncher = createTestLauncher(edifactExportJob);

    JobExecution jobExecution = testLauncher.startStep("saveToMinIOStep", getJobParameters(), getExecutionContext());

    assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
  }

  @Test
  @DirtiesContext
  void minioUploadFails()
    throws IOException {
    JobOperatorTestUtils testLauncher = createTestLauncher(edifactExportJob);
    doThrow(new NullPointerException()).when(remoteFilesStorage).write(anyString(), any(byte[].class));

    JobExecution jobExecution = testLauncher.startStep("saveToMinIOStep", getJobParameters(), getExecutionContext());

    assertEquals(ExitStatus.FAILED.getExitCode(), jobExecution.getExitStatus().getExitCode());
  }


  private JobParameters getJobParameters() throws IOException {
    JobParametersBuilder paramsBuilder = new JobParametersBuilder();

    paramsBuilder.addString(EDIFACT_ORDERS_EXPORT, getMockData("edifact/edifactOrdersExport.json"));
    paramsBuilder.addString(ACQ_EXPORT_FILE, RandomStringUtils.secure().next(100, true, true));
    paramsBuilder.addString(JOB_ID, UUID.randomUUID().toString());

    return paramsBuilder.toJobParameters();
  }

  private ExecutionContext getExecutionContext() {
    ExecutionContext executionContext = new ExecutionContext();
    executionContext.put(ACQ_EXPORT_FILE, RandomStringUtils.secure().next(100, true, true));
    return executionContext;
  }

}
