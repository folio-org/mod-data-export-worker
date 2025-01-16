package org.folio.dew.batch.acquisitions.services;

import static org.folio.dew.batch.acquisitions.services.ResendService.EDIFACT_ORDERS_EXPORT_KEY;
import static org.folio.dew.batch.acquisitions.services.ResendService.FILE_NAME_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.UUID;

import org.folio.de.entity.Job;
import org.folio.de.entity.JobCommand;
import org.folio.dew.CopilotGenerated;
import org.folio.dew.batch.acquisitions.exceptions.EdifactException;
import org.folio.dew.config.kafka.KafkaService;
import org.folio.dew.domain.dto.EdiFtp;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig.TransmissionMethodEnum;
import org.folio.dew.repository.RemoteFilesStorage;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;

import com.fasterxml.jackson.databind.ObjectMapper;

@CopilotGenerated(partiallyGenerated = true)
@ExtendWith(MockitoExtension.class)
class ResendServiceTest {

  @InjectMocks
  private ResendService resendService;
  @Mock
  private RemoteFilesStorage remoteFilesStorage;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private FTPStorageService ftpStorageService;
  @Mock
  private KafkaService kafka;
  @Mock
  private ObjectMapper ediObjectMapper;

  @Test
  void resendExportedFile_shouldThrowException_whenJobIdIsNull() {
    JobCommand jobCommand = new JobCommand();
    jobCommand.setJobParameters(new JobParameters());

    EdifactException exception = assertThrows(EdifactException.class, () -> resendService.resendExportedFile(jobCommand));

    assertEquals("Job update with empty Job ID.", exception.getMessage());
  }

  @Test
  void resendExportedFile_shouldThrowException_whenTransmissionMethodIsNotFtp() throws Exception {
    var jobId = UUID.randomUUID();
    JobCommand jobCommand = new JobCommand();
    jobCommand.setId(jobId);
    jobCommand.setJobParameters(new JobParameters(Map.of(EDIFACT_ORDERS_EXPORT_KEY, new JobParameter<>("", String.class))));

    VendorEdiOrdersExportConfig config = new VendorEdiOrdersExportConfig();
    config.setTransmissionMethod(TransmissionMethodEnum.FILE_DOWNLOAD);
    config.setIntegrationType(VendorEdiOrdersExportConfig.IntegrationTypeEnum.CLAIMING);
    when(ediObjectMapper.readValue(anyString(), eq(VendorEdiOrdersExportConfig.class))).thenReturn(config);

    resendService.resendExportedFile(jobCommand);

    var expectedFailedJob = createFailedJob(jobId, ExportType.CLAIMS, "Transmission method must be FTP in order to resend exported file (EdifactException)");
    verify(kafka).send(KafkaService.Topic.JOB_UPDATE, jobId.toString(), expectedFailedJob);
  }

  @Test
  void resendExportedFile_shouldThrowException_whenFtpFieldsAreMissing() throws Exception {
    var jobId = UUID.randomUUID();
    JobCommand jobCommand = new JobCommand();
    jobCommand.setId(jobId);
    jobCommand.setJobParameters(new JobParameters(Map.of(EDIFACT_ORDERS_EXPORT_KEY, new JobParameter<>("", String.class))));

    VendorEdiOrdersExportConfig config = new VendorEdiOrdersExportConfig();
    config.setTransmissionMethod(TransmissionMethodEnum.FTP);
    config.setIntegrationType(VendorEdiOrdersExportConfig.IntegrationTypeEnum.CLAIMING);
    config.setEdiFtp(new EdiFtp());
    when(ediObjectMapper.readValue(anyString(), eq(VendorEdiOrdersExportConfig.class))).thenReturn(config);

    resendService.resendExportedFile(jobCommand);

    var expectedFailedJob = createFailedJob(jobId, ExportType.CLAIMS, "Export configuration is incomplete, missing required fields: [ftpPort, serverAddress] (EdifactException)");
    verify(kafka).send(KafkaService.Topic.JOB_UPDATE, jobId.toString(), expectedFailedJob);
  }

  @Test
  void resendExportedFile_shouldCompleteSuccessfully_whenAllConditionsAreMet() throws Exception {
    var fileName = "fileName";
    Map<String, JobParameter<?>> jobParamMap = Map.of(
      EDIFACT_ORDERS_EXPORT_KEY, new JobParameter<>("", String.class),
      FILE_NAME_KEY, new JobParameter<>(fileName, String.class)
    );
    JobCommand jobCommand = new JobCommand();
    jobCommand.setId(UUID.randomUUID());
    jobCommand.setJobParameters(new JobParameters(jobParamMap));

    VendorEdiOrdersExportConfig config = new VendorEdiOrdersExportConfig();
    config.setTransmissionMethod(TransmissionMethodEnum.FTP);
    config.setIntegrationType(VendorEdiOrdersExportConfig.IntegrationTypeEnum.CLAIMING);
    config.setEdiFtp(new EdiFtp().ftpFormat(EdiFtp.FtpFormatEnum.FTP).ftpPort(21).serverAddress("localhost"));
    when(ediObjectMapper.readValue(anyString(), eq(VendorEdiOrdersExportConfig.class))).thenReturn(config);
    when(remoteFilesStorage.readAllBytes(anyString())).thenReturn(new byte[0]);

    resendService.resendExportedFile(jobCommand);

    verify(ftpStorageService).uploadToFtp(eq(config), any(byte[].class), eq(fileName));
    verify(kafka).send(eq(KafkaService.Topic.JOB_UPDATE), anyString(), any());
  }

  private static Job createFailedJob(UUID jobId, ExportType exportType, String errorDetails) {
    Job job = new Job();
    job.setId(jobId);
    job.setType(exportType);
    job.setErrorDetails(errorDetails);
    job.setBatchStatus(BatchStatus.FAILED);
    job.setExitStatus(ExitStatus.FAILED);
    return job;
  }

}
