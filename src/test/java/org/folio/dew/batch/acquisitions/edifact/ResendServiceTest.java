package org.folio.dew.batch.acquisitions.edifact;

import static org.folio.dew.utils.TestUtils.getMockData;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.folio.de.entity.JobCommand;
import org.folio.dew.BaseBatchTest;
import org.folio.dew.batch.acquisitions.edifact.exceptions.EdifactException;
import org.folio.dew.batch.acquisitions.edifact.services.ResendService;
import org.folio.dew.batch.acquisitions.edifact.services.SaveToFTPStorageService;
import org.folio.dew.config.kafka.KafkaService;
import org.folio.dew.domain.dto.JobParameterNames;
import org.folio.dew.repository.RemoteFilesStorage;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class ResendServiceTest extends BaseBatchTest {

  @MockBean
  private RemoteFilesStorage remoteFilesStorage;
  @MockBean
  private SaveToFTPStorageService saveToFTPStorageService;
  @MockBean
  private KafkaService kafka;
  @Autowired
  private ResendService resendService;

  private final static String EMPTY_ID = "";
  private final static String ID = UUID.randomUUID().toString();
  private static final String EDIFACT_ORDERS_EXPORT_KEY = "EDIFACT_ORDERS_EXPORT";
  private static final String FILE_NAME_KEY = "FILE_NAME";

  @Test
  public void shouldFailedResend() {
    doNothing().when(acknowledgment).acknowledge();
    Exception exception = assertThrows(EdifactException.class, () -> {
      resendService.resendExportedFile(getJobCommand(EMPTY_ID), acknowledgment);
    });
    String expectedMessage = "Job update with empty Job ID.";
    String actualMessage = exception.getMessage();

    assertTrue(actualMessage.contains(expectedMessage));
  }

  @Test
  public void shouldResendSuccessful() throws Exception {
    doNothing().when(acknowledgment).acknowledge();
    doNothing().when(kafka).send(any(), anyString(), any());
    doNothing().when(saveToFTPStorageService).uploadToFtp(any(), anyString(), anyString());
    String exportedFile = getMockData("edifact/edifactFTPOrdersExport.json");
    doReturn(exportedFile.getBytes()).when(remoteFilesStorage).readAllBytes(anyString());

    resendService.resendExportedFile(getJobCommand(ID), acknowledgment);

    verify(remoteFilesStorage, times(1)).readAllBytes(anyString());
    verify(saveToFTPStorageService, times(1)).uploadToFtp(any(), anyString(), anyString());
  }

  private JobCommand getJobCommand(String id) throws IOException {
    JobCommand jobCommand = new JobCommand();
    Map<String, JobParameter> params = new HashMap<>();
    params.put(JobParameterNames.JOB_ID, new JobParameter(id));
    params.put(FILE_NAME_KEY, new JobParameter("testExportedFile.csv"));
    params.put(EDIFACT_ORDERS_EXPORT_KEY, new JobParameter(getMockData("edifact/edifactFTPOrdersExport.json")));
    jobCommand.setJobParameters(new JobParameters(params));

    return jobCommand;
  }
}
