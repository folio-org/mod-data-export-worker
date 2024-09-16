package org.folio.dew.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

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
