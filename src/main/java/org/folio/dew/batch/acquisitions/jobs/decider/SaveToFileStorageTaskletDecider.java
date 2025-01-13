package org.folio.dew.batch.acquisitions.jobs.decider;

import static org.folio.dew.domain.dto.VendorEdiOrdersExportConfig.IntegrationTypeEnum.ORDERING;
import static org.folio.dew.domain.dto.VendorEdiOrdersExportConfig.TransmissionMethodEnum.FTP;

import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class SaveToFileStorageTaskletDecider extends ExportStepDecider {

  public SaveToFileStorageTaskletDecider(ObjectMapper objectMapper, String stepName) {
    super(objectMapper, stepName);
  }

  @Override
  public ExportStepDecision decide(VendorEdiOrdersExportConfig exportConfig, JobExecution jobExecution, StepExecution stepExecution) {
    // Always execute if the integration type is ORDERING, or other integration type if the transmission method is FTP
    if (exportConfig.getIntegrationType() == ORDERING || exportConfig.getTransmissionMethod() == FTP) {
      return ExportStepDecision.PROCESS;
    }
    log.info("decide:: Transmission method is not FTP, skipping the step: {}", stepName);
    return ExportStepDecision.SKIP;
  }

}
