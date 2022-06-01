package org.folio.dew.service;

import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.stereotype.Service;

@JobScope
@Service
public class BulkEditStatisticService {

  private final BulkEditStatistic statistic = new BulkEditStatistic();

  public void incrementSuccess() {
      statistic.setSuccess(statistic.getSuccess() + 1);
  }

  public void incrementErrors() {
    statistic.setErrors(statistic.getErrors() + 1);
  }

  public BulkEditStatistic getStatistic() {
    return statistic;
  }
}
