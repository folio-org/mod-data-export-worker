package org.folio.dew.batch.acquisitions.jobs.decider;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;

import static org.folio.dew.domain.dto.VendorEdiOrdersExportConfig.IntegrationTypeEnum.ORDERING;
import static org.folio.dew.domain.dto.VendorEdiOrdersExportConfig.TransmissionMethodEnum.EMAIL;


@Log4j2
public class SendToEmailTaskletDecider extends ExportStepDecider {

  public SendToEmailTaskletDecider(ObjectMapper objectMapper, String stepName) {
    super(objectMapper, stepName);
  }

  @Override
  public ExportStepDecision decide(VendorEdiOrdersExportConfig exportConfig, JobExecution jobExecution, StepExecution stepExecution) {
    // Always execute if the integration type is ORDERING, or execute for other integration type if the transmission method is Email
    if (exportConfig.getIntegrationType() == ORDERING || exportConfig.getTransmissionMethod() == EMAIL) {
      log.info("decide:: Processing step: {}", stepName);
      return ExportStepDecision.PROCESS;
    }
    log.info("decide:: Integration type is not ORDERING or Transmission method is not FTP, skipping the step: {}", stepName);
    return ExportStepDecision.SKIP;
  }

}
