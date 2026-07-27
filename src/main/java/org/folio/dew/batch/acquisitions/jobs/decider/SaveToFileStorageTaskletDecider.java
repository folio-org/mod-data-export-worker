package org.folio.dew.batch.acquisitions.jobs.decider;

import static org.folio.dew.domain.dto.VendorEdiOrdersExportConfig.TransmissionMethodEnum.FTP;

import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.step.StepExecution;

@Log4j2
public class SaveToFileStorageTaskletDecider extends ExportStepDecider {

  public SaveToFileStorageTaskletDecider(ObjectMapper objectMapper, String stepName) {
    super(objectMapper, stepName);
  }

  @Override
  public ExportStepDecision decide(VendorEdiOrdersExportConfig exportConfig, JobExecution jobExecution, StepExecution stepExecution) {
    if (exportConfig.getTransmissionMethod() == FTP) {
      log.info("decide:: Processing step: {}", stepName);
      return ExportStepDecision.PROCESS;
    }
    log.info("decide:: Transmission method is not FTP, skipping the step: {}", stepName);
    return ExportStepDecision.SKIP;
  }

}
