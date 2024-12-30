package org.folio.dew.batch.acquisitions.edifact.jobs;

import static org.folio.dew.domain.dto.JobParameterNames.ACQ_EXPORT_FILE;
import static org.folio.dew.domain.dto.JobParameterNames.ACQ_EXPORT_FILE_NAME;
import static org.folio.dew.domain.dto.VendorEdiOrdersExportConfig.IntegrationTypeEnum.ORDERING;
import static org.folio.dew.domain.dto.VendorEdiOrdersExportConfig.TransmissionMethodEnum.FTP;

import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;
import org.folio.dew.batch.ExecutionContextUtils;
import org.folio.dew.batch.acquisitions.edifact.services.FTPStorageService;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;

@SuperBuilder
@Component
@StepScope
@Log4j2
public class SaveToFileStorageTasklet extends FilterableTasklet {

  private static final String SFTP_PROTOCOL = "sftp://";

  private final ObjectMapper ediObjectMapper;
  private final FTPStorageService ftpStorageService;

  @Override
  @SneakyThrows
  public RepeatStatus execute(VendorEdiOrdersExportConfig exportConfig, StepContribution contribution, ChunkContext chunkContext) {
    String host = exportConfig.getEdiFtp().getServerAddress().replace(SFTP_PROTOCOL, "");
    // skip ftp upload if address not specified
    if (StringUtils.isEmpty(host)) {
      return RepeatStatus.FINISHED;
    }

    var stepExecution = chunkContext.getStepContext().getStepExecution();
    var fileName = (String) ExecutionContextUtils.getExecutionVariable(stepExecution, ACQ_EXPORT_FILE_NAME);
    var edifactOrderAsString = (String) ExecutionContextUtils.getExecutionVariable(stepExecution, ACQ_EXPORT_FILE);
    ftpStorageService.uploadToFtp(exportConfig,  edifactOrderAsString.getBytes(StandardCharsets.UTF_8), fileName);

    return RepeatStatus.FINISHED;
  }

  @Override
  protected boolean shouldExecute(VendorEdiOrdersExportConfig exportConfig) {
    // Always execute if the integration type is not ORDERING, or execute for other integration types if the transmission method is FTP
    if (exportConfig.getIntegrationType() == ORDERING || exportConfig.getTransmissionMethod() == FTP) {
      return true;
    }
    log.info("execute:: Transmission method is not FTP, skipping the step");
    return false;
  }

}
