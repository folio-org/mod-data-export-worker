package org.folio.dew.batch.acquisitions.edifact.jobs.decider;

import static org.folio.dew.domain.dto.VendorEdiOrdersExportConfig.IntegrationTypeEnum.ORDERING;
import static org.folio.dew.domain.dto.VendorEdiOrdersExportConfig.TransmissionMethodEnum.FTP;

import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;

@SuperBuilder
@Component
@StepScope
@Log4j2
public class SaveToFileStorageTaskletDecider extends ExportStepDecider {

  @Override
  public ExportStepDecision decide(VendorEdiOrdersExportConfig exportConfig, JobExecution jobExecution, StepExecution stepExecution) {
    // Always execute if the integration type is ORDERING, or other integration type if the transmission method is FTP
    if (exportConfig.getIntegrationType() == ORDERING || exportConfig.getTransmissionMethod() == FTP) {
      return ExportStepDecision.PROCESS;
    }
    log.info("decide:: Transmission method is not FTP, skipping the step: {}", stepExecution.getStepName());
    return ExportStepDecision.SKIP;
  }

}
