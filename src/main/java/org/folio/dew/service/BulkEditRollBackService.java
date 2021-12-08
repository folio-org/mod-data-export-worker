package org.folio.dew.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.batch.operations.JobOperator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BulkEditRollBackService {

  private Map<UUID, Long> executionsPerJob = new HashMap<>();

  @Autowired
  private JobOperator jobOperator;

  public void stopJobExecution(UUID jobId) {
    jobOperator.stop(executionsPerJob.remove(jobId));
  }

  public void putExecutionPerJob(UUID jobId, long executionId) {
    executionsPerJob.put(jobId, executionId);
  }
}
