package org.folio.dew.service;

import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@JobScope
@Service
public class BulkEditUpdateStatisticService {

  private Map<UUID, BulkEditStatistic> data = new HashMap<>();

  public void incrementSuccess(UUID jobId) {
    var statistic = data.get(jobId);
    if (statistic == null) {
      statistic = new BulkEditStatistic();
      statistic.setSuccess(1);
      data.put(jobId, statistic);
    } else {
      statistic.setSuccess(statistic.getSuccess() + 1);
    }
  }

  public void incrementErrors(UUID jobId) {
    var statistic = data.get(jobId);
    if (statistic == null) {
      statistic = new BulkEditStatistic();
      statistic.setErrors(1);
      data.put(jobId, statistic);
    } else {
      statistic.setErrors(statistic.getErrors() + 1);
    }
  }

  public BulkEditStatistic getStatistic(UUID jobId) {
    var statistic = data.get(jobId);
    if (statistic == null) return new BulkEditStatistic();
    return statistic;
  }

  public void cleanJobData(UUID jobId) {
    data.remove(jobId);
  }
}
