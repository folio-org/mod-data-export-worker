package org.folio.dew.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.client.DataExportSpringClient;
import org.folio.dew.repository.MinIOObjectStorageRepository;
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

import static org.folio.dew.utils.Constants.PATH_SEPARATOR;
import static org.folio.dew.utils.Constants.TMP_DIR_PROPERTY;

@Service
@RequiredArgsConstructor
@Log4j2
public class BulkEditRollBackService {

  private static final String UPDATE_JOB_DONE_MESSAGE = "Update job already has been done, can not be stopped";
  private static final String ROLLBACK_JOB_DONE_MESSAGE = "Rollback has been done";

  private final Map<UUID, Long> executionIdPerJobId = new HashMap<>();
  private final Map<UUID, Set<String>> usersIdsToRollBackForJobId = new HashMap<>();
  private final Map<UUID, String> jobIdIdsWithRollBackFilePerJobId = new HashMap<>();

  private String workDir;
  @Value("${spring.application.name}")
  private String springApplicationName;
  private final JobOperator jobOperator;
  @Autowired
  @Qualifier("bulkEditRollBackJob")
  private Job job;
  private final BulkEditStopJobLauncher stopJobLauncher;
  private final DataExportSpringClient dataExportSpringClient;
  private final MinIOObjectStorageRepository minIOObjectStorageRepository;

  @PostConstruct
  public void postConstruct() {
    workDir = System.getProperty(TMP_DIR_PROPERTY) + PATH_SEPARATOR + springApplicationName + PATH_SEPARATOR;
  }

  public String stopAndRollBackJobExecutionByJobId(UUID jobId) {
    try {
      if (executionIdPerJobId.containsKey(jobId)) {
        log.info("Rollback for jobId {} is started", jobId.toString());
        jobOperator.stop(executionIdPerJobId.get(jobId));
        rollBackExecutionByJobId(jobId);
        return ROLLBACK_JOB_DONE_MESSAGE;
      }
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return UPDATE_JOB_DONE_MESSAGE;
  }

  public void putExecutionInfoPerJob(long executionId, UUID jobId, String fileUploadName) {
    executionIdPerJobId.put(jobId, executionId);
    jobIdIdsWithRollBackFilePerJobId.put(jobId, getJobIdFromFileName(fileUploadName));
  }

  public void putUserIdForJob(String userId, UUID jobId) {
    var existUsersIds = usersIdsToRollBackForJobId.computeIfAbsent(jobId, key -> new HashSet<>());
    existUsersIds.add(userId);
  }

  public boolean isUserIdExistForJob(String userId, UUID jobId) {
    return usersIdsToRollBackForJobId.get(jobId) != null
      && usersIdsToRollBackForJobId.get(jobId).contains(userId);
  }

  public boolean isExecutionIdExistForJob(UUID jobId) {
    return executionIdPerJobId.containsKey(jobId);
  }

  public boolean isJobIdWithRollBackFileExistForJob(UUID jobId) {
    return jobIdIdsWithRollBackFilePerJobId.containsKey(jobId);
  }

  public void cleanJobData(String exitCode, UUID jobId) {
    if (!ExitStatus.STOPPED.getExitCode().equals(exitCode)) {
      executionIdPerJobId.remove(jobId);
      usersIdsToRollBackForJobId.remove(jobId);
      jobIdIdsWithRollBackFilePerJobId.remove(jobId);
    }
  }

  public void cleanJobData(UUID jobId) {
    executionIdPerJobId.remove(jobId);
    usersIdsToRollBackForJobId.remove(jobId);
    jobIdIdsWithRollBackFilePerJobId.remove(jobId);
  }

  private String getJobIdFromFileName(String fileUploadName) {
    return StringUtils.substringAfterLast(StringUtils.substringBefore(fileUploadName, "_"), PATH_SEPARATOR);
  }

  private void rollBackExecutionByJobId(UUID jobId) throws Exception {
    var jobIdWithRollBackFile = jobIdIdsWithRollBackFilePerJobId.get(jobId);
    var fileToRollBack = workDir + jobIdWithRollBackFile + "_origin.csv";
    var fileToRollBackMinIOPath = dataExportSpringClient.getJobById(jobIdWithRollBackFile).getFiles().get(0);
    var objectName = getObjectName(fileToRollBackMinIOPath);
    minIOObjectStorageRepository.downloadObject(objectName, fileToRollBack);
    var parameters = new HashMap<String, JobParameter>();
    parameters.put(Constants.JOB_ID, new JobParameter(jobId.toString()));
    parameters.put(Constants.FILE_NAME, new JobParameter(fileToRollBack));
    stopJobLauncher.run(job, new JobParameters(parameters));
  }

  private String getObjectName(String path) {
    String[] splitted = path.split("/");
    return StringUtils.substringBeforeLast(splitted[splitted.length - 1], ".csv") + ".csv";
  }
}
