package org.folio.dew.service;

import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BulkEditChangedRecordsService {
  private final Map<String, Set<String>> changedIdsMap = new ConcurrentHashMap<>();

  public void addUserId(String userId, String jobId) {
    var ids = changedIdsMap.computeIfAbsent(jobId, key -> new HashSet<>());
    ids.add(userId);
  }

  public void removeUserId(String userId, String jobId) {
    var ids = changedIdsMap.getOrDefault(jobId, new HashSet<>());
    ids.remove(userId);
  }

  public Set<String> fetchChangedUserIds(String jobId) {
    return changedIdsMap.remove(jobId);
  }
}
