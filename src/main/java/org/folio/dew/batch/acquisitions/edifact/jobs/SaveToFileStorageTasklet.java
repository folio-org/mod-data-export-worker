package org.folio.dew.batch.acquisitions.edifact.jobs;

import static org.folio.dew.domain.dto.JobParameterNames.EDIFACT_ORDER_EXPORT;

import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;
import org.folio.dew.batch.acquisitions.edifact.services.FTPStorageService;
import org.folio.dew.domain.dto.EdiFtp;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.dew.repository.LocalFilesStorage;
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
  private final FTPStorageService ftpStorageService;

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
    var ediExportConfig = objectMapper.readValue((String)jobParameters.get(EDIFACT_ORDER_EXPORT), VendorEdiOrdersExportConfig.class);

    String host = ediExportConfig.getEdiFtp().getServerAddress().replace(SFTP_PROTOCOL, "");
    // skip ftp upload if address not specified
    if (StringUtils.isEmpty(host)) {
      return RepeatStatus.FINISHED;
    }

    if (EdiFtp.FtpFormatEnum.SFTP.equals(ediExportConfig.getEdiFtp().getFtpFormat())) {
      var uploadedFile = new String(localFilesStorage.readAllBytes(uploadedFilePath), StandardCharsets.UTF_8);
      ftpStorageService.uploadToFtp(ediExportConfig, uploadedFile, edifactFileName);
    } else {
      ftpStorageService.uploadToFtp(ediExportConfig, edifactOrderAsString, edifactFileName);
    }

    return RepeatStatus.FINISHED;
  }

}
