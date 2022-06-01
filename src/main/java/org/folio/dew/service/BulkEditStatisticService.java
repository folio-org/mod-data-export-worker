package org.folio.dew.service;

import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.stereotype.Service;

@JobScope
@Service
public class BulkEditStatisticService {

  private BulkEditStatistic statistic = new BulkEditStatistic();

  public void incrementSuccess() {
      statistic.setSuccess(statistic.getSuccess() + 1);
  }

  public void incrementSuccess(int value) {
    statistic.setSuccess(statistic.getSuccess() + value);
  }
  public void incrementErrors() {
    statistic.setErrors(statistic.getErrors() + 1);
  }

  public BulkEditStatistic getStatistic() {
    return statistic;
  }

  public void clear() {
    statistic = new BulkEditStatistic();
  }
}
