package org.folio.dew.batch.acquisitions.jobs.decider;

import static org.folio.dew.domain.dto.VendorEdiOrdersExportConfig.IntegrationTypeEnum.ORDERING;

import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class ExportHistoryTaskletDecider extends ExportStepDecider {

  public ExportHistoryTaskletDecider(ObjectMapper objectMapper, String stepName) {
    super(objectMapper, stepName);
  }

  @Override
  public ExportStepDecision decide(VendorEdiOrdersExportConfig exportConfig, JobExecution jobExecution, StepExecution stepExecution) {
    // Always execute if the integration type is ORDERING
    if (exportConfig.getIntegrationType() == ORDERING) {
      log.info("decide:: Processing step: {}", stepName);
      return ExportStepDecision.PROCESS;
    }
    log.info("decide:: Integration type is not ORDERING, skipping the step: {}", stepName);
    return ExportStepDecision.SKIP;
  }

}
