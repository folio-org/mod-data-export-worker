package org.folio.dew.batch.acquisitions.edifact.jobs;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.folio.dew.batch.ExecutionContextUtils;
import org.folio.dew.domain.dto.EdiFtp;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.dew.repository.FTPObjectStorageRepository;
import org.folio.dew.repository.SFTPObjectStorageRepository;
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
  private final SFTPObjectStorageRepository sftpObjectStorageRepository;
  private final FTPObjectStorageRepository ftpObjectStorageRepository;

  private static final String SFTP_PROTOCOL = "sftp://";

  @Override
  @SneakyThrows
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    var stepExecutionContext = chunkContext.getStepContext().getStepExecution();

    var jobParameters = chunkContext.getStepContext().getJobParameters();
    var ediExportConfig = objectMapper.readValue((String)jobParameters.get("edifactOrdersExport"), VendorEdiOrdersExportConfig.class);

    String username = ediExportConfig.getEdiFtp().getUsername();
    String folder = ediExportConfig.getEdiFtp().getOrderDirectory();
    String password = ediExportConfig.getEdiFtp().getPassword();
    String host = ediExportConfig.getEdiFtp().getServerAddress().replace(SFTP_PROTOCOL, "");
    int port = ediExportConfig.getEdiFtp().getFtpPort();
    String filename = UUID.randomUUID() +  ".edi";

    // skip ftp upload if address not specified
    if (StringUtils.isEmpty(host)) {
      return RepeatStatus.FINISHED;
    }

    var fileContent = (String) ExecutionContextUtils.getExecutionVariable(stepExecutionContext,"edifactOrderAsString");

    if (ediExportConfig.getEdiFtp().getFtpFormat().equals(EdiFtp.FtpFormatEnum.SFTP)) {
      sftpObjectStorageRepository.upload(username, password, host, port, folder, filename, fileContent);
    }
    else {
      ftpObjectStorageRepository.login(host, username,password);
      ftpObjectStorageRepository.upload(filename, fileContent);
    }

    return RepeatStatus.FINISHED;
  }
}
