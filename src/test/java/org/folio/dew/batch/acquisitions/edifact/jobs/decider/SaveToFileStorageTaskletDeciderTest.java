package org.folio.dew.batch.acquisitions.edifact.jobs.decider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import org.folio.dew.CopilotGenerated;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig.IntegrationTypeEnum;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig.TransmissionMethodEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;

import com.fasterxml.jackson.databind.ObjectMapper;

@CopilotGenerated
class SaveToFileStorageTaskletDeciderTest {

  private SaveToFileStorageTaskletDecider decider;

  @BeforeEach
  void setUp() {
    decider = new SaveToFileStorageTaskletDecider(new ObjectMapper());
  }

  @Test
  void decide_shouldProcess_whenIntegrationTypeIsOrdering() {
    VendorEdiOrdersExportConfig config = new VendorEdiOrdersExportConfig();
    config.setIntegrationType(IntegrationTypeEnum.ORDERING);
    JobExecution jobExecution = mock(JobExecution.class);
    StepExecution stepExecution = mock(StepExecution.class);

    ExportStepDecision decision = decider.decide(config, jobExecution, stepExecution);

    assertEquals(ExportStepDecision.PROCESS, decision);
  }

  @Test
  void decide_shouldProcess_whenTransmissionMethodIsFtp() {
    VendorEdiOrdersExportConfig config = new VendorEdiOrdersExportConfig();
    config.setIntegrationType(IntegrationTypeEnum.CLAIMING);
    config.setTransmissionMethod(TransmissionMethodEnum.FTP);
    JobExecution jobExecution = mock(JobExecution.class);
    StepExecution stepExecution = mock(StepExecution.class);

    ExportStepDecision decision = decider.decide(config, jobExecution, stepExecution);

    assertEquals(ExportStepDecision.PROCESS, decision);
  }

  @Test
  void decide_shouldSkip_whenIntegrationTypeIsNotOrderingAndTransmissionMethodIsNotFtp() {
    VendorEdiOrdersExportConfig config = new VendorEdiOrdersExportConfig();
    config.setIntegrationType(IntegrationTypeEnum.CLAIMING);
    config.setTransmissionMethod(TransmissionMethodEnum.FILE_DOWNLOAD);
    JobExecution jobExecution = mock(JobExecution.class);
    StepExecution stepExecution = mock(StepExecution.class);

    ExportStepDecision decision = decider.decide(config, jobExecution, stepExecution);

    assertEquals(ExportStepDecision.SKIP, decision);
  }
}
