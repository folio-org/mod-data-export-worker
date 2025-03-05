package org.folio.dew.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

import static java.util.Optional.ofNullable;

@Service
public class BulkEditStatisticService {

  private final Map<String, LongAdder> success;

  public BulkEditStatisticService() {
    success = new ConcurrentHashMap<>();
  }

  public void incrementSuccess(String jobId, int value) {
    success.computeIfAbsent(jobId, val -> new LongAdder()).add(value);
  }

  public int getSuccess(String jobId) {
    return ofNullable(success.get(jobId)).map(LongAdder::intValue).orElse(0);
  }

  public void reset(String jobId) {
    success.remove(jobId);
  }
}
