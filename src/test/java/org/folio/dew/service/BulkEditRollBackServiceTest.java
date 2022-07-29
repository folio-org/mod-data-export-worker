package org.folio.dew.service;

import org.folio.dew.client.DataExportSpringClient;
import org.folio.dew.repository.RemoteFilesStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobOperator;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BulkEditRollBackServiceTest {

  @Mock
  private JobOperator jobOperator;
  @Mock
  private BulkEditRollBackJobLauncher rollBackJobLauncher;
  @Mock
  private DataExportSpringClient dataExportSpringClient;
  @Mock
  private RemoteFilesStorage remoteFilesStorage;

  @InjectMocks
  private BulkEditRollBackService bulkEditRollBackService;

  @Test
  void stopAndRollBackJobExecutionByJobIdTest() throws Exception {
    var jobId = UUID.fromString("edd30136-9a7b-4226-9e82-83024dbeac4a");
    var jobIdWithRollBackFile = jobId.toString();
    var executionId = 0l;
    var job = new org.folio.dew.domain.dto.Job();
    job.setFiles(List.of("minio/path/" + jobIdWithRollBackFile + "_file.csv"));

    bulkEditRollBackService.putExecutionInfoPerJob(executionId, jobId);
    when(dataExportSpringClient.getJobById(isA(String.class))).thenReturn(job);
    doNothing().when(remoteFilesStorage).downloadObject(isA(String.class), isA(String.class));
    when(rollBackJobLauncher.run(any(), isA(JobParameters.class))).thenReturn(new JobExecution(1l));

    bulkEditRollBackService.stopAndRollBackJobExecutionByJobId(jobId);

    verify(jobOperator, times(1)).stop(executionId);
    verify(dataExportSpringClient, times(1)).getJobById(jobIdWithRollBackFile);
    verify(remoteFilesStorage, times(1)).downloadObject(isA(String.class), isA(String.class));
    verify(rollBackJobLauncher, times(1)).run(any(), isA(JobParameters.class));
  }

  @Test
  void getFileForRollBackFromMinIO() {
    var jobIdWithRollBackFile = "74914e57-3406-4757-938b-9a3f718d0ee6";
    var fileUploadName = "/some/file/" + jobIdWithRollBackFile + "_file.csv";
    var job = new org.folio.dew.domain.dto.Job();
    job.setFiles(List.of("minio/path/" + jobIdWithRollBackFile + "_file.csv"));

    when(dataExportSpringClient.getJobById(isA(String.class))).thenReturn(job);

    var actual = bulkEditRollBackService.getFileForRollBackFromMinIO(fileUploadName);
    assertEquals("minio/path/74914e57-3406-4757-938b-9a3f718d0ee6_file.csv", actual);
  }

  @Test
  void cleanJobDataTest() {
    var jobId = UUID.fromString("edd30136-9a7b-4226-9e82-83024dbeac4a");
    var jobIdWithRollBackFile = "74914e57-3406-4757-938b-9a3f718d0ee6";
    var executionId = 0l;
    var userId = "userId";

    bulkEditRollBackService.putExecutionInfoPerJob(executionId, jobId);
    bulkEditRollBackService.putUserIdForJob(userId, jobId);
    assertTrue(bulkEditRollBackService.isExecutionIdExistForJob(jobId));
    assertTrue(bulkEditRollBackService.isUserBeRollBack(userId, jobId));

    bulkEditRollBackService.cleanJobData(jobId);
    assertFalse(bulkEditRollBackService.isExecutionIdExistForJob(jobId));
    assertTrue(bulkEditRollBackService.isUserBeRollBack(userId, jobId));
  }

  @Test
  void cleanJobDataWithCompletedExitCodeTest() {
    var jobId = UUID.fromString("edd30136-9a7b-4226-9e82-83024dbeac4a");
    var jobIdWithRollBackFile = "74914e57-3406-4757-938b-9a3f718d0ee6";
    var executionId = 0l;
    var userId = "userId";

    bulkEditRollBackService.putExecutionInfoPerJob(executionId, jobId);
    bulkEditRollBackService.putUserIdForJob(userId, jobId);
    assertTrue(bulkEditRollBackService.isExecutionIdExistForJob(jobId));
    assertTrue(bulkEditRollBackService.isUserBeRollBack(userId, jobId));

    bulkEditRollBackService.cleanJobData(ExitStatus.COMPLETED.getExitCode(), jobId);
    assertFalse(bulkEditRollBackService.isExecutionIdExistForJob(jobId));
    assertTrue(bulkEditRollBackService.isUserBeRollBack(userId, jobId));
  }

  @Test
  void cleanJobDataWithStoppedExitCodeTest() {
    var jobId = UUID.fromString("edd30136-9a7b-4226-9e82-83024dbeac4a");
    var jobIdWithRollBackFile = "74914e57-3406-4757-938b-9a3f718d0ee6";
    var executionId = 0l;
    var userId = "userId";

    bulkEditRollBackService.putExecutionInfoPerJob(executionId, jobId);
    bulkEditRollBackService.putUserIdForJob(userId, jobId);
    assertTrue(bulkEditRollBackService.isExecutionIdExistForJob(jobId));

    bulkEditRollBackService.cleanJobData(ExitStatus.STOPPED.getExitCode(), jobId);
    assertTrue(bulkEditRollBackService.isExecutionIdExistForJob(jobId));
    assertTrue(bulkEditRollBackService.isUserBeRollBack(userId, jobId));
    assertFalse(bulkEditRollBackService.isUserBeRollBack(userId, jobId));
  }
}
