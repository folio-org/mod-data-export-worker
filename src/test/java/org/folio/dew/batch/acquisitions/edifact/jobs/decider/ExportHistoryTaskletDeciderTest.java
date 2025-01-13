package org.folio.dew.batch.acquisitions.edifact.jobs.decider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import org.folio.dew.CopilotGenerated;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig.IntegrationTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;

import com.fasterxml.jackson.databind.ObjectMapper;

@CopilotGenerated
class ExportHistoryTaskletDeciderTest {

  private ExportHistoryTaskletDecider decider;

  @BeforeEach
  void setUp() {
    decider = new ExportHistoryTaskletDecider(new ObjectMapper(), "createExportHistoryRecordsStep");
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
  void decide_shouldSkip_whenIntegrationTypeIsNotOrdering() {
    VendorEdiOrdersExportConfig config = new VendorEdiOrdersExportConfig();
    config.setIntegrationType(IntegrationTypeEnum.CLAIMING);
    JobExecution jobExecution = mock(JobExecution.class);
    StepExecution stepExecution = mock(StepExecution.class);

    ExportStepDecision decision = decider.decide(config, jobExecution, stepExecution);

    assertEquals(ExportStepDecision.SKIP, decision);
  }

}
