package org.folio.dew.batch.bulkedit.jobs;

import org.folio.dew.domain.dto.ItemIdentifier;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DuplicationChecker {

  private final Set<ItemIdentifier> identifiersToCheckDuplication = ConcurrentHashMap.newKeySet();
  private final Set<String> fetchedInstanceIds = ConcurrentHashMap.newKeySet();

  public boolean isDuplicate(ItemIdentifier itemIdentifier) {
    return !identifiersToCheckDuplication.add(itemIdentifier);
  }

  public boolean fetched(String id) {
    return !fetchedInstanceIds.add(id);
  }

  public void reset() {
    identifiersToCheckDuplication.clear();
    fetchedInstanceIds.clear();
  }
}
