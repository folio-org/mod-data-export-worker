package org.folio.dew.batch.authoritycontrol;

import lombok.SneakyThrows;
import org.folio.dew.repository.LocalFilesStorage;
import org.folio.dew.repository.RemoteFilesStorage;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;

import java.io.IOException;
import java.util.HashMap;

import static org.folio.dew.domain.dto.JobParameterNames.TEMP_OUTPUT_FILE_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorityControlStepListenerTest {
  @Mock
  private RemoteFilesStorage remoteFilesStorage;
  @Mock
  private LocalFilesStorage localFilesStorage;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @InjectMocks
  private AuthorityControlStepListener stepListener;

  @Test
  @SneakyThrows
  void shouldProcessFailedAfterStepExecution() {
    var jobExecution = mock(JobExecution.class);
    var stepExecution = mock(StepExecution.class);
    var exitStatus = new ExitStatus("200");
    var parameters = new HashMap<String, JobParameter>();
    parameters.put(TEMP_OUTPUT_FILE_PATH, new JobParameter("any"));

    when(localFilesStorage.notExists("any")).thenReturn(false);
    when(folioExecutionContext.getTenantId()).thenReturn("test");
    doThrow(new IOException()).when(remoteFilesStorage)
      .uploadObject(anyString(), anyString(), anyString(), anyString(), anyBoolean());

    when(stepExecution.getExitStatus()).thenReturn(exitStatus);
    when(stepExecution.getJobExecution()).thenReturn(jobExecution);
    when(stepExecution.getJobParameters()).thenReturn(new JobParameters(parameters));

    var result = stepListener.afterStepExecution(stepExecution);

    assertEquals(ExitStatus.FAILED, result);
  }

  @Test
  void shouldProcessFailedAfterStepExecution_ifFileNotExist() {
    var stepExecution = mock(StepExecution.class);
    var exitStatus = new ExitStatus("200");
    var parameters = new HashMap<String, JobParameter>();
    parameters.put(TEMP_OUTPUT_FILE_PATH, new JobParameter("any"));

    when(localFilesStorage.notExists("any")).thenReturn(true);

    when(stepExecution.getExitStatus()).thenReturn(exitStatus);
    when(stepExecution.getJobParameters()).thenReturn(new JobParameters(parameters));

    var result = stepListener.afterStepExecution(stepExecution);

    assertEquals(ExitStatus.FAILED, result);
  }
}
