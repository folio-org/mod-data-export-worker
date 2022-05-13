package org.folio.dew.batch.acquisitions.edifact.jobs;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.folio.dew.batch.ExecutionContextUtils;
import org.folio.dew.batch.acquisitions.edifact.exceptions.EdifactException;
import org.folio.dew.batch.acquisitions.edifact.services.OrganizationsService;
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
  private final OrganizationsService organizationsService;

  private static final String SFTP_PROTOCOL = "sftp://";

  @Override
  @SneakyThrows
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    var stepExecution = chunkContext.getStepContext().getStepExecution();

    var jobParameters = chunkContext.getStepContext().getJobParameters();
    var ediExportConfig = objectMapper.readValue((String)jobParameters.get("edifactOrdersExport"), VendorEdiOrdersExportConfig.class);

    String username = ediExportConfig.getEdiFtp().getUsername();
    String folder = ediExportConfig.getEdiFtp().getOrderDirectory();
    String password = ediExportConfig.getEdiFtp().getPassword();
    String host = ediExportConfig.getEdiFtp().getServerAddress().replace(SFTP_PROTOCOL, "");
    String filename = generateFileName(ediExportConfig);
    Optional<Integer> port = Optional.ofNullable(ediExportConfig.getEdiFtp().getFtpPort());

    // skip ftp upload if address not specified
    if (StringUtils.isEmpty(host)) {
      return RepeatStatus.FINISHED;
    }

    var fileContent = (String) ExecutionContextUtils.getExecutionVariable(stepExecution,"edifactOrderAsString");

    if (port.isEmpty()) {
      throw new EdifactException("Export configuration is incomplete, missing FTP/SFTP Port");
    }

    if (ediExportConfig.getEdiFtp().getFtpFormat().equals(EdiFtp.FtpFormatEnum.SFTP)) {
      sftpObjectStorageRepository.upload(username, password, host, port.get(), folder, filename, fileContent);
    }
    else {
      ftpObjectStorageRepository.login(host, username,password);
      ftpObjectStorageRepository.upload(filename, fileContent);
    }
    ExecutionContextUtils.addToJobExecutionContext(stepExecution, "edifactFileName", filename, "");

    return RepeatStatus.FINISHED;
  }

  private String generateFileName(VendorEdiOrdersExportConfig ediExportConfig) {
    var orgName = organizationsService.getOrganizationById(ediExportConfig.getVendorId().toString()).get("code").asText();
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
    var fileDate = dateFormat.format(new Date());
    // exclude restricted symbols after implementing naming convention feature
    return orgName + "_" + ediExportConfig.getConfigName() + "_" + fileDate + ".edi";
  }
}
