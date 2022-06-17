package org.folio.dew.service;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Log4j2
public class BulkEditChangedRecordsService {
  private final Map<String, Set<String>> changedIdsMap = new ConcurrentHashMap<>();

  public void addUserId(String userId, String jobId) {
    var ids = changedIdsMap.computeIfAbsent(jobId, key -> new HashSet<>());
    ids.add(userId);
  }

  public Set<String> fetchChangedUserIds(String jobId) {
    return changedIdsMap.remove(jobId);
  }
}
