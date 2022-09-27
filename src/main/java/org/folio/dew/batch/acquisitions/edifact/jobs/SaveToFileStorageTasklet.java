package org.folio.dew.batch.acquisitions.edifact.jobs;

import org.apache.commons.lang3.StringUtils;
import org.folio.dew.batch.ExecutionContextUtils;
import org.folio.dew.batch.acquisitions.edifact.services.SaveToFileStorageService;
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

@RequiredArgsConstructor
@Component
@StepScope
@Log4j2
public class SaveToFileStorageTasklet implements Tasklet {
  private final ObjectMapper objectMapper;
  private final SaveToFileStorageService saveToFileStorageService;

  private static final String SFTP_PROTOCOL = "sftp://";

  @Override
  @SneakyThrows
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    var stepExecution = chunkContext.getStepContext().getStepExecution();

    var jobParameters = chunkContext.getStepContext().getJobParameters();
    var ediExportConfig = objectMapper.readValue((String)jobParameters.get("edifactOrdersExport"), VendorEdiOrdersExportConfig.class);

    String host = ediExportConfig.getEdiFtp().getServerAddress().replace(SFTP_PROTOCOL, "");

    // skip ftp upload if address not specified
    if (StringUtils.isEmpty(host)) {
      return RepeatStatus.FINISHED;
    }

    var fileContent = (String) ExecutionContextUtils.getExecutionVariable(stepExecution,"edifactOrderAsString");
    String filename = saveToFileStorageService.uploadToFtp(ediExportConfig, fileContent);

    ExecutionContextUtils.addToJobExecutionContext(stepExecution, "edifactFileName", filename, "");
    return RepeatStatus.FINISHED;
  }
}
