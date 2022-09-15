package org.folio.dew.service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.client.DataExportSpringClient;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.repository.RemoteFilesStorage;
import org.folio.dew.utils.Constants;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.springframework.batch.core.launch.JobOperator;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.folio.dew.utils.Constants.JOB_ID_SEPARATOR;
import static org.folio.dew.utils.Constants.PATH_SEPARATOR;
import static org.folio.dew.utils.Constants.TMP_DIR_PROPERTY;

@Service
@RequiredArgsConstructor
@Log4j2
public class BulkEditRollBackService {

  private static final String ROLLBACK_ERROR_MESSAGE = "Rollback error";
  private static final String ROLLBACK_DONE_MESSAGE = "Rollback has been done";

  private final Map<UUID, Long> executionIdPerJobId = new ConcurrentHashMap<>();
  private final Map<UUID, Set<String>> usersIdsToRollBackForJobId = new ConcurrentHashMap<>();

  private String workDir;
  @Value("${spring.application.name}")
  private String springApplicationName;
  private final JobOperator jobOperator;
  @Autowired
  @Qualifier("bulkEditRollBackJob")
  private Job job;
  private final BulkEditRollBackJobLauncher rollBackJobLauncher;
  private final DataExportSpringClient dataExportSpringClient;
  private final RemoteFilesStorage remoteFilesStorage;

  @PostConstruct
  public void postConstruct() {
    workDir = System.getProperty(TMP_DIR_PROPERTY) + PATH_SEPARATOR + springApplicationName + PATH_SEPARATOR;
  }

  public String stopAndRollBackJobExecutionByJobId(UUID jobId) {
    try {
      log.info("Rollback for jobId {} is started", jobId.toString());
      if (executionIdPerJobId.containsKey(jobId)) {
        jobOperator.stop(executionIdPerJobId.get(jobId));
      }
      rollBackByJobId(jobId);
      return ROLLBACK_DONE_MESSAGE;
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return ROLLBACK_ERROR_MESSAGE;
  }

  public void putExecutionInfoPerJob(long executionId, UUID jobId) {
    executionIdPerJobId.put(jobId, executionId);
  }

  public void putUserIdForJob(String userId, UUID jobId) {
    var existUsersIds = usersIdsToRollBackForJobId.computeIfAbsent(jobId, key -> new HashSet<>());
    existUsersIds.add(userId);
  }

  public boolean isUserBeRollBack(String userId, UUID jobId) {
    return !executionIdPerJobId.containsKey(jobId) || (usersIdsToRollBackForJobId.get(jobId) != null
      && usersIdsToRollBackForJobId.get(jobId).remove(userId));
  }

  public boolean isExecutionIdExistForJob(UUID jobId) {
    return executionIdPerJobId.containsKey(jobId);
  }
  public void cleanJobData(String exitCode, UUID jobId) {
    if (!ExitStatus.STOPPED.getExitCode().equals(exitCode)) {
      executionIdPerJobId.remove(jobId);
      usersIdsToRollBackForJobId.remove(jobId);
    }
  }

  public void cleanJobData(UUID jobId) {
    executionIdPerJobId.remove(jobId);
    usersIdsToRollBackForJobId.remove(jobId);
  }

  @SneakyThrows
  public String getFileForRollBackFromMinIO(String fileUploadName) {
    var jobId = getJobIdFromFileName(fileUploadName);
    var files = dataExportSpringClient.getJobById(jobId).getFiles();
    if (files.isEmpty()) {
      var error = "Rollback file does not exist for job " + jobId;
      throw new BulkEditException(error);
    }
    return files.get(0);
  }

  private String getJobIdFromFileName(String fileUploadName) {
    return StringUtils.substringBefore(fileUploadName, JOB_ID_SEPARATOR);
  }

  @SneakyThrows
  private void rollBackByJobId(UUID jobId) {
    var fileForRollBack = workDir + jobId.toString() + "_rollBack.csv";
    var files = dataExportSpringClient.getJobById(jobId.toString()).getFiles();
    if (files.isEmpty()) {
      var error = "Rollback file does not exist for job " + jobId.toString();
      throw new BulkEditException(error);
    }
    var fileForRollBackMinIOPath = files.get(0);
    var objectName = getObjectName(fileForRollBackMinIOPath);
    remoteFilesStorage.downloadObject(objectName, fileForRollBack);
    rollBackJobLauncher.run(job, getRollBackParameters(jobId.toString(), fileForRollBack));
  }

  private JobParameters getRollBackParameters(String jobId, String fileToRollBack) {
    var parameters = new HashMap<String, JobParameter>();
    parameters.put(Constants.JOB_ID, new JobParameter(jobId));
    parameters.put(Constants.FILE_NAME, new JobParameter(fileToRollBack));
    return new JobParameters(parameters);
  }

  private String getObjectName(String path) {
    String[] splitted = path.split("/");
    return StringUtils.substringBeforeLast(splitted[splitted.length - 1], ".csv") + ".csv";
  }
}
