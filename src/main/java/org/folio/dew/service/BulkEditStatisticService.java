package org.folio.dew.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class BulkEditStatisticService {

  private Map<String, AtomicInteger> success = new ConcurrentHashMap<>();

  public void incrementSuccess(String jobId, int value) {
    success.get(jobId).addAndGet(value);
  }

  public int getSuccess(String jobId) {
    return success.get(jobId).get();
  }

  public void reset(String jobId) {
    success.remove(jobId);
  }
}
