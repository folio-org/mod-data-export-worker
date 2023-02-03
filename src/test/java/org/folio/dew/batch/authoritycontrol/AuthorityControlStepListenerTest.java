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
import org.springframework.batch.item.ExecutionContext;

import java.io.IOException;
import java.util.HashMap;

import static org.folio.dew.domain.dto.JobParameterNames.AUTHORITY_CONTROL_FILE_NAME;
import static org.folio.dew.domain.dto.JobParameterNames.OUTPUT_FILES_IN_STORAGE;
import static org.folio.dew.domain.dto.JobParameterNames.TEMP_OUTPUT_FILE_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorityControlStepListenerTest {
  private static final String TEST_TENANT = "test";
  private static final String TEMP_FILE = "temp_file";
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
  void shouldProcessAfterStepExecution() {
    var jobExecution = mock(JobExecution.class);
    var stepExecution = mock(StepExecution.class);
    var executionContext = mock(ExecutionContext.class);
    var parameters = new HashMap<String, JobParameter>();
    parameters.put(TEMP_OUTPUT_FILE_PATH, new JobParameter(TEMP_FILE));

    when(stepExecution.getExitStatus()).thenReturn(ExitStatus.COMPLETED);
    when(stepExecution.getJobParameters()).thenReturn(new JobParameters(parameters));
    when(stepExecution.getJobExecution()).thenReturn(jobExecution);
    when(jobExecution.getExecutionContext()).thenReturn(executionContext);
    when(localFilesStorage.notExists(any())).thenReturn(false);
    when(folioExecutionContext.getTenantId()).thenReturn(TEST_TENANT);
    doReturn(TEMP_FILE).when(remoteFilesStorage)
      .uploadObject(anyString(), anyString(), eq(null), anyString(), anyBoolean());

    var result = stepListener.afterStepExecution(stepExecution);

    verify(executionContext).putString(AUTHORITY_CONTROL_FILE_NAME, TEMP_FILE);
    verify(executionContext).putString(OUTPUT_FILES_IN_STORAGE, TEMP_FILE);
    assertEquals(ExitStatus.COMPLETED, result);
  }

  @Test
  @SneakyThrows
  void shouldFailedAfterStepExecution_ifUploadWasFailed() {
    var jobExecution = mock(JobExecution.class);
    var stepExecution = mock(StepExecution.class);
    var parameters = new HashMap<String, JobParameter>();
    parameters.put(TEMP_OUTPUT_FILE_PATH, new JobParameter(TEMP_FILE));

    when(stepExecution.getJobExecution()).thenReturn(jobExecution);
    when(stepExecution.getJobParameters()).thenReturn(new JobParameters(parameters));
    when(localFilesStorage.notExists(any())).thenReturn(false);
    when(folioExecutionContext.getTenantId()).thenReturn(TEST_TENANT);
    doThrow(new IOException()).when(remoteFilesStorage)
      .uploadObject(anyString(), anyString(), eq(null), anyString(), anyBoolean());

    var result = stepListener.afterStepExecution(stepExecution);

    verify(jobExecution).addFailureException(any());
    assertEquals(ExitStatus.FAILED, result);
  }

  @Test
  void shouldFailedAfterStepExecution_ifTempFileNotExist() {
    var jobExecution = mock(JobExecution.class);
    var stepExecution = mock(StepExecution.class);
    var parameters = new HashMap<String, JobParameter>();
    parameters.put(TEMP_OUTPUT_FILE_PATH, new JobParameter("tempFile"));

    when(stepExecution.getJobExecution()).thenReturn(jobExecution);
    when(stepExecution.getJobParameters()).thenReturn(new JobParameters(parameters));
    when(localFilesStorage.notExists(any())).thenReturn(true);

    var result = stepListener.afterStepExecution(stepExecution);

    verify(jobExecution).addFailureException(any());
    assertEquals(ExitStatus.FAILED, result);
  }
}
