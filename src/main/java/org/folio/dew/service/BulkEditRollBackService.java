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

  private Map<UUID, Long> executionIdPerJobId = new HashMap<>();
  private Map<UUID, Set<String>> usersIdsToRollBackForJobId = new HashMap<>();

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

  public void stopAndRollBackJobExecutionByJobId(UUID jobId) {
    try {
      if (executionIdPerJobId.containsKey(jobId)) {
        log.info("Roll-back for jobId {} is started", jobId.toString());
        jobOperator.stop(executionIdPerJobId.get(jobId));
        rollBackExecutionByJobId(jobId);
      }
    } catch (Exception e) {
      log.error(e.getMessage());
    }
  }

  public void putExecutionPerJob(long executionId, UUID jobId) {
    executionIdPerJobId.put(jobId, executionId);
  }

  public void putUserIdForJob(String userId, UUID jobId) {
    var existUsersIds = usersIdsToRollBackForJobId.get(jobId);
    if (existUsersIds == null) {
      existUsersIds = new HashSet<>();
      usersIdsToRollBackForJobId.put(jobId, existUsersIds);
    }
    existUsersIds.add(userId);
  }

  public boolean isUserIdExistForJob(String userId, UUID jobId) {
    return usersIdsToRollBackForJobId.get(jobId) != null
      && usersIdsToRollBackForJobId.get(jobId).contains(userId);
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

  private void rollBackExecutionByJobId(UUID jobId) throws Exception {
    var fileToSave = workDir + jobId.toString() + "_origin.csv";
    var objectPath = dataExportSpringClient.getJobById(jobId.toString()).getFiles().get(0);
    var objectName = getObjectName(objectPath);
    minIOObjectStorageRepository.downloadObject(objectName, fileToSave);
    var parameters = new HashMap<String, JobParameter>();
    parameters.put(Constants.JOB_ID, new JobParameter(jobId.toString()));
    parameters.put(Constants.FILE_NAME, new JobParameter(fileToSave));
    stopJobLauncher.run(job, new JobParameters(parameters));
  }

  private String getObjectName(String path) {
    String[] splitted = path.split("/");
    return StringUtils.substringBeforeLast(splitted[splitted.length - 1], ".csv") + ".csv";
  }
}
