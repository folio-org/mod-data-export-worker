package org.folio.dew.batch.acquisitions.edifact.jobs;

import static org.folio.dew.domain.dto.JobParameterNames.EDIFACT_ORDERS_EXPORT;
import static org.folio.dew.domain.dto.JobParameterNames.UPLOADED_FILE_PATH;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.batch.ExecutionContextUtils;
import org.folio.dew.batch.acquisitions.edifact.services.FTPStorageService;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.dew.repository.RemoteFilesStorage;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
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
  private final ObjectMapper ediObjectMapper;
  private final FTPStorageService ftpStorageService;

  private final RemoteFilesStorage remoteFilesStorage;

  private static final String SFTP_PROTOCOL = "sftp://";
  @Value("#{jobParameters['edifactFileName']}")
  private String edifactFileName;

  @Override
  @SneakyThrows
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    var stepExecution = chunkContext.getStepContext().getStepExecution();
    var jobParameters = chunkContext.getStepContext().getJobParameters();
    var ediExportConfig = ediObjectMapper.readValue((String)jobParameters.get(EDIFACT_ORDERS_EXPORT), VendorEdiOrdersExportConfig.class);
    var uploadedFilePath = (String) ExecutionContextUtils.getExecutionVariable(stepExecution, UPLOADED_FILE_PATH);

    String host = ediExportConfig.getEdiFtp().getServerAddress().replace(SFTP_PROTOCOL, "");
    // skip ftp upload if address not specified
    if (StringUtils.isEmpty(host)) {
      return RepeatStatus.FINISHED;
    }
    byte[] fileContent = remoteFilesStorage.readAllBytes(uploadedFilePath);
    ftpStorageService.uploadToFtp(ediExportConfig, fileContent, FilenameUtils.getName(uploadedFilePath));

    return RepeatStatus.FINISHED;
  }

}
