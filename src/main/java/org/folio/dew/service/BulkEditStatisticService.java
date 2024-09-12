package org.folio.dew.service;

import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

//@StepScope // remove
@Service
public class BulkEditStatisticService {

  private AtomicInteger success = new AtomicInteger();

  public void incrementSuccess(int value) {
    success.addAndGet(value);
  }

  public int getSuccess() {
    return success.get();
  }

  public void reset() {
    success.set(0);
  }
}
