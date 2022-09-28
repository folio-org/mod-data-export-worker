package org.folio.dew.batch.acquisitions.edifact.jobs;

import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;
import org.folio.dew.domain.dto.EdiFtp;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.dew.repository.FTPObjectStorageRepository;
import org.folio.dew.repository.LocalFilesStorage;
import org.folio.dew.repository.SFTPObjectStorageRepository;
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
  private final ObjectMapper objectMapper;
  private final SFTPObjectStorageRepository sftpObjectStorageRepository;
  private final FTPObjectStorageRepository ftpObjectStorageRepository;

  private final LocalFilesStorage localFilesStorage;

  private static final String SFTP_PROTOCOL = "sftp://";
  @Value("#{jobParameters['edifactFileName']}")
  private String edifactFileName;

  @Value("#{jobParameters['edifactOrderAsString']}")
  private String edifactOrderAsString;
  @Value("#{jobParameters['uploadedFilePath']}")
  private String uploadedFilePath;

  @Override
  @SneakyThrows
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {

    var jobParameters = chunkContext.getStepContext().getJobParameters();
    var ediExportConfig = objectMapper.readValue((String)jobParameters.get("edifactOrdersExport"), VendorEdiOrdersExportConfig.class);

    String username = ediExportConfig.getEdiFtp().getUsername();
    String folder = ediExportConfig.getEdiFtp().getOrderDirectory();
    String password = ediExportConfig.getEdiFtp().getPassword();
    String host = ediExportConfig.getEdiFtp().getServerAddress().replace(SFTP_PROTOCOL, "");
    int port = ediExportConfig.getEdiFtp().getFtpPort();

    // skip ftp upload if address not specified
    if (StringUtils.isEmpty(host)) {
      return RepeatStatus.FINISHED;
    }


    if (ediExportConfig.getEdiFtp().getFtpFormat().equals(EdiFtp.FtpFormatEnum.SFTP)) {
      try {
        var uploadedFile = new String(localFilesStorage.readAllBytes(uploadedFilePath), StandardCharsets.UTF_8);
        sftpObjectStorageRepository.upload(username, password, host, port, folder, edifactFileName, uploadedFile);
      }
      finally {
        localFilesStorage.delete(uploadedFilePath);
      }
    }
    else {
      ftpObjectStorageRepository.login(host, username, password);
      ftpObjectStorageRepository.upload(edifactFileName, edifactOrderAsString);
    }

    return RepeatStatus.FINISHED;
  }

}
