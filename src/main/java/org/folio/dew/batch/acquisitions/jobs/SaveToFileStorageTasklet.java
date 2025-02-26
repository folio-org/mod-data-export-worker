package org.folio.dew.batch.acquisitions.jobs;

import static org.folio.dew.domain.dto.JobParameterNames.ACQ_EXPORT_FILE;
import static org.folio.dew.domain.dto.JobParameterNames.ACQ_EXPORT_FILE_NAME;
import static org.folio.dew.domain.dto.JobParameterNames.EDIFACT_ORDERS_EXPORT;

import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;
import org.folio.dew.batch.ExecutionContextUtils;
import org.folio.dew.batch.acquisitions.services.FTPStorageService;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Log4j2
@StepScope
@Component
@RequiredArgsConstructor
public class SaveToFileStorageTasklet implements Tasklet {

  private static final String SFTP_PROTOCOL = "sftp://";

  private final ObjectMapper ediObjectMapper;
  private final FTPStorageService ftpStorageService;

  @Override
  @SneakyThrows
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    var exportConfig = ediObjectMapper.readValue((String) chunkContext.getStepContext().getJobParameters().get(EDIFACT_ORDERS_EXPORT), VendorEdiOrdersExportConfig.class);
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

}
