package org.folio.dew.batch.acquisitions.edifact.services;

import static org.folio.dew.utils.Constants.EDIFACT_EXPORT_DIR_NAME;
import static org.folio.dew.utils.Constants.getWorkingDirectory;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.de.entity.Job;
import org.folio.de.entity.JobCommand;
import org.folio.dew.batch.acquisitions.edifact.exceptions.EdifactException;
import org.folio.dew.config.kafka.KafkaService;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.ExportTypeSpecificParameters;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.dew.repository.RemoteFilesStorage;
import org.folio.spring.FolioExecutionContext;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobParameters;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class ResendService {

  private final RemoteFilesStorage remoteFilesStorage;
  private final FolioExecutionContext folioExecutionContext;
  private final FTPStorageService ftpStorageService;
  private final KafkaService kafka;
  private final ObjectMapper ediObjectMapper;

  private static final String EDIFACT_ORDERS_EXPORT_KEY = "EDIFACT_ORDERS_EXPORT";
  private static final String FILE_NAME_KEY = "FILE_NAME";

  @Value("${spring.application.name}")
  protected String springApplicationName;

  public void resendExportedFile(JobCommand jobCommand) {
    UUID jobId = jobCommand.getId();
    JobParameters jobParameters = jobCommand.getJobParameters();

    if (jobId == null) {
      log.error("Job update with empty Job ID {}.", jobCommand);
      throw new EdifactException("Job update with empty Job ID.");
    }
    log.info("Resend operation started for job ID: {}", jobId);
    Job job = new Job();
    job.setType(ExportType.EDIFACT_ORDERS_EXPORT);
    job.setId(jobId);
    try {
      String fileName = jobParameters.getString(FILE_NAME_KEY);
      VendorEdiOrdersExportConfig ediConfig = ediObjectMapper.readValue(jobParameters.getString(EDIFACT_ORDERS_EXPORT_KEY),
        VendorEdiOrdersExportConfig.class);

      String workDir = getWorkingDirectory(springApplicationName, EDIFACT_EXPORT_DIR_NAME);
      String tenantName = folioExecutionContext.getTenantId();
      String path = String.format("%s%s/%s", workDir, tenantName, fileName);

      byte[] exportFile = remoteFilesStorage.readAllBytes(path);
      ftpStorageService.uploadToFtp(ediConfig, exportFile, fileName);

      ExportTypeSpecificParameters parameters = new ExportTypeSpecificParameters();
      parameters.setVendorEdiOrdersExportConfig(ediConfig);
      job.setExportTypeSpecificParameters(parameters);
      job.setBatchStatus(BatchStatus.COMPLETED);
      job.setExitStatus(ExitStatus.COMPLETED);
      log.info("Resend operation finished for job ID: {}", jobId);
    } catch (Exception e) {
      log.error("Resending failed with an error for job Id: {}", jobId, e);
      job.setErrorDetails(getThrowableRootCauseDetails(e));
      job.setBatchStatus(BatchStatus.FAILED);
      job.setExitStatus(ExitStatus.FAILED);
    } finally {
      kafka.send(KafkaService.Topic.JOB_UPDATE, jobId.toString(), job);
    }
  }

  private String getThrowableRootCauseDetails(Throwable t) {
    Throwable cause = t.getCause();
    while (cause != null && cause != t) {
      t = cause;
      cause = t.getCause();
    }
    return t.getMessage() + " (" + t.getClass()
      .getSimpleName() + ')';
  }
}
