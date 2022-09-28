package org.folio.dew.batch.acquisitions.edifact.services;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.folio.de.entity.JobCommand;
import org.folio.dew.batch.acquisitions.edifact.exceptions.EdifactException;
import org.folio.dew.config.kafka.KafkaService;
import org.folio.dew.domain.dto.Job;
import org.folio.dew.domain.dto.JobParameterNames;
import org.folio.dew.domain.dto.JobStatus;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.dew.repository.RemoteFilesStorage;
import org.folio.spring.FolioExecutionContext;
import org.springframework.batch.core.JobParameters;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class ResendService {

  private final RemoteFilesStorage remoteFilesStorage;
  private final FolioExecutionContext folioExecutionContext;
  private final SaveToFTPStorageService saveToFTPStorageService;
  private final ObjectMapper objectMapper;
  private final KafkaService kafka;

  private static final String EDIFACT_ORDERS_EXPORT_KEY = "EDIFACT_ORDERS_EXPORT";
  private static final String FILE_NAME_KEY = "FILE_NAME";

  public void resendExportedFile(JobCommand jobCommand, Acknowledgment acknowledgment) {
    JobParameters jobParameters = jobCommand.getJobParameters();
    String jobId = jobParameters.getString(JobParameterNames.JOB_ID);
    if (StringUtils.isBlank(jobId)) {
      log.error("Job update with empty Job ID {}.", jobCommand);
      throw new EdifactException("Job update with empty Job ID.");
    }

    log.info("Resend operation started");
    Job job = new Job();
    job.setId(UUID.fromString(jobId));
    try {
      acknowledgment.acknowledge();

      String fileName = jobParameters.getString(FILE_NAME_KEY);
      VendorEdiOrdersExportConfig ediConfig = objectMapper.readValue(jobParameters.getString(EDIFACT_ORDERS_EXPORT_KEY),
          VendorEdiOrdersExportConfig.class);
      String tenantName = folioExecutionContext.getTenantId();
      String path = String.format("%s/%s", tenantName, fileName);

      byte[] exportFile = remoteFilesStorage.readAllBytes(path);
      saveToFTPStorageService.uploadToFtp(ediConfig, new String(exportFile), fileName);
      job.setStatus(JobStatus.SUCCESSFUL);
      log.info("Resend operation finished");
    } catch (Exception e) {
      log.error("Resending failed with an error", e);
      job.setErrorDetails(getThrowableRootCauseDetails(e));
      job.setStatus(JobStatus.FAILED);
    }

    kafka.send(KafkaService.Topic.JOB_UPDATE, jobId, job);
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
