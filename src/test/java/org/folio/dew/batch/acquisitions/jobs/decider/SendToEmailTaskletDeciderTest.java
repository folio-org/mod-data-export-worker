package org.folio.dew.batch.acquisitions.jobs.decider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import org.folio.dew.CopilotGenerated;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig.IntegrationTypeEnum;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig.TransmissionMethodEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.step.StepExecution;

import com.fasterxml.jackson.databind.ObjectMapper;

@CopilotGenerated
class SendToEmailTaskletDeciderTest {

  private SendToEmailTaskletDecider decider;

  @BeforeEach
  void setUp() {
    decider = new SendToEmailTaskletDecider(new ObjectMapper(), "sendToEmailStep");
  }

  @Test
  void decide_shouldProcess_whenIntegrationTypeIsClaiming() {
    VendorEdiOrdersExportConfig config = new VendorEdiOrdersExportConfig();
    config.setIntegrationType(IntegrationTypeEnum.CLAIMING);
    JobExecution jobExecution = mock(JobExecution.class);
    StepExecution stepExecution = mock(StepExecution.class);

    ExportStepDecision decision = decider.decide(config, jobExecution, stepExecution);

    assertEquals(ExportStepDecision.PROCESS, decision);
  }

  @Test
  void decide_shouldProcess_whenTransmissionMethodIsEmail() {
    VendorEdiOrdersExportConfig config = new VendorEdiOrdersExportConfig();
    config.setIntegrationType(IntegrationTypeEnum.ORDERING);
    config.setTransmissionMethod(TransmissionMethodEnum.EMAIL);
    JobExecution jobExecution = mock(JobExecution.class);
    StepExecution stepExecution = mock(StepExecution.class);

    ExportStepDecision decision = decider.decide(config, jobExecution, stepExecution);

    assertEquals(ExportStepDecision.PROCESS, decision);
  }

  @Test
  void decide_shouldSkip_whenIntegrationTypeIsNotClaimingAndTransmissionMethodIsNotEmail() {
    VendorEdiOrdersExportConfig config = new VendorEdiOrdersExportConfig();
    config.setIntegrationType(IntegrationTypeEnum.ORDERING);
    config.setTransmissionMethod(TransmissionMethodEnum.FTP);
    JobExecution jobExecution = mock(JobExecution.class);
    StepExecution stepExecution = mock(StepExecution.class);

    ExportStepDecision decision = decider.decide(config, jobExecution, stepExecution);

    assertEquals(ExportStepDecision.SKIP, decision);
  }
}
