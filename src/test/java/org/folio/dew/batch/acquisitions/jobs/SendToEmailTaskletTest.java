package org.folio.dew.batch.acquisitions.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dew.domain.dto.JobParameterNames.ACQ_EXPORT_FILE;
import static org.folio.dew.domain.dto.JobParameterNames.ACQ_EXPORT_FILE_NAME;
import static org.folio.dew.domain.dto.JobParameterNames.EDIFACT_ORDERS_EXPORT;
import static org.folio.dew.domain.dto.JobParameterNames.JOB_ID;
import static org.folio.dew.utils.TestUtils.getMockData;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import java.io.IOException;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
import org.folio.dew.BaseBatchTest;
import org.folio.dew.batch.acquisitions.services.OrganizationsService;
import org.folio.dew.domain.dto.acquisitions.edifact.Organization;
import org.folio.dew.client.EmailClient;
import org.folio.dew.client.TemplateEngineClient;
import org.folio.dew.domain.dto.templateengine.TemplateProcessingResponse;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import lombok.SneakyThrows;

class SendToEmailTaskletTest extends BaseBatchTest {

  @Autowired
  @Qualifier("edifactOrdersExportJob")
  private Job edifactExportJob;
  @MockitoBean
  private EmailClient emailClient;
  @MockitoBean
  private TemplateEngineClient templateEngineClient;
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
    doReturn(organization).when(organizationsService).getOrganizationById(any());
    doNothing().when(emailClient).sendEmail(any());

    var templateResult = new TemplateProcessingResponse();
    var result = new TemplateProcessingResponse.Result();
    result.setHeader("Test Subject");
    result.setBody("Test Body");
    var meta = new TemplateProcessingResponse.Meta();
    meta.setOutputFormat("text/html");
    templateResult.setResult(result);
    templateResult.setMeta(meta);
    doReturn(templateResult).when(templateEngineClient).processTemplate(any());
  }

  @Test
  @DirtiesContext
  void testSendEmailSuccessful() throws Exception {
    JobOperatorTestUtils testLauncher = createTestLauncher(edifactExportJob);

    var jobParameters = getJobParameters("edifact/edifactEmailOrdersExport.json");
    ExecutionContext executionContext = getExecutionContext();
    JobExecution jobExecution = testLauncher.startStep("sendToEmailStep", jobParameters, executionContext);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
  }

  @Test
  @DirtiesContext
  void testSendEmailSuccessful_forOrderingIntegrationType() throws Exception {
    JobOperatorTestUtils testLauncher = createTestLauncher(edifactExportJob);

    var jobParameters = getJobParameters("edifact/edifactEmailOrdersExportOrdering.json");
    ExecutionContext executionContext = getExecutionContext();
    JobExecution jobExecution = testLauncher.startStep("sendToEmailStep", jobParameters, executionContext);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
  }

  @Test
  @DirtiesContext
  void testSendEmail_shouldFail_whenTemplateEngineThrows() throws Exception {
    doThrow(new RuntimeException("template-engine unavailable")).when(templateEngineClient).processTemplate(any());
    JobOperatorTestUtils testLauncher = createTestLauncher(edifactExportJob);

    var jobParameters = getJobParameters("edifact/edifactEmailOrdersExport.json");
    ExecutionContext executionContext = getExecutionContext();
    JobExecution jobExecution = testLauncher.startStep("sendToEmailStep", jobParameters, executionContext);

    assertThat(jobExecution.getExitStatus().getExitCode()).isEqualTo(ExitStatus.FAILED.getExitCode());
  }

  @Test
  @DirtiesContext
  void testSendEmail_shouldFail_whenEmailClientThrows() throws Exception {
    doThrow(new RuntimeException("email service unavailable")).when(emailClient).sendEmail(any());
    JobOperatorTestUtils testLauncher = createTestLauncher(edifactExportJob);

    var jobParameters = getJobParameters("edifact/edifactEmailOrdersExport.json");
    ExecutionContext executionContext = getExecutionContext();
    JobExecution jobExecution = testLauncher.startStep("sendToEmailStep", jobParameters, executionContext);

    assertThat(jobExecution.getExitStatus().getExitCode()).isEqualTo(ExitStatus.FAILED.getExitCode());
  }

  private JobParameters getJobParameters(String edifactOrdersExport) throws IOException {
    JobParametersBuilder paramsBuilder = new JobParametersBuilder();
    paramsBuilder.addString(EDIFACT_ORDERS_EXPORT, getMockData(edifactOrdersExport));
    paramsBuilder.addString(JOB_ID, UUID.randomUUID().toString());
    return paramsBuilder.toJobParameters();
  }

  private ExecutionContext getExecutionContext() {
    ExecutionContext executionContext = new ExecutionContext();
    executionContext.put(ACQ_EXPORT_FILE_NAME, "testEdiFile.edi");
    executionContext.put(ACQ_EXPORT_FILE, RandomStringUtils.secure().next(100));
    return executionContext;
  }


}
