package org.folio.dew.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.batch.ExportJobManager;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.integration.launch.JobLaunchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import org.springframework.batch.core.launch.JobOperator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class BulkEditRollBackService {

  private Map<UUID, Long> executionIdPerJobId = new HashMap<>();
  private Map<UUID, Set<String>> usersIdsToRollBackForJobId = new HashMap<>();

  private final ExportJobManager exportJobManager;
  private final JobOperator jobOperator;
  @Autowired
  @Qualifier("bulkEditRollBackJob")
  private Job job;

  public void stopJobExecution(UUID jobId) {
    try {
      if (executionIdPerJobId.containsKey(jobId)) {
        var parameters = new HashMap<String, JobParameter>();
        parameters.put("jobId", new JobParameter(jobId.toString()));
        //ToDo find file
        jobOperator.stop(executionIdPerJobId.get(jobId));
        var jobLaunchRequest = new JobLaunchRequest(job, new JobParameters(parameters));
        exportJobManager.launchJob(jobLaunchRequest);
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
    }
    existUsersIds.add(userId);
  }

  public boolean isUserIdExistForJob(String userId, UUID jobId) {
    return usersIdsToRollBackForJobId.get(jobId) != null
      && usersIdsToRollBackForJobId.get(jobId).contains(userId);
  }

  public void cleanJobData(UUID jobId) {
    executionIdPerJobId.remove(jobId);
    usersIdsToRollBackForJobId.remove(jobId);
  }
}
