package org.folio.dew.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.batch.ExportJobManager;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.integration.launch.JobLaunchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.batch.operations.JobOperator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class BulkEditRollBackService {

  private static Map<UUID, Long> executionIdPerJobId = new HashMap<>();
  private static Map<UUID, Set<String>> usersIdsToRollBackForJobId = new HashMap<>();

  private final ExportJobManager exportJobManager;
  private final JobOperator jobOperator;
  @Autowired
  @Qualifier("bulk-edit-roll-back")
  private final Job job;

  public void stopJobExecution(UUID jobId) {
    try {
      if (executionIdPerJobId.containsKey(jobId)) {
        // ToDo File to roll-back
        jobOperator.stop(executionIdPerJobId.get(jobId));
        var jobLaunchRequest = new JobLaunchRequest(job, new JobParameters());
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
