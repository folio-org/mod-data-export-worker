package org.folio.dew.batch.bulkedit.jobs;

import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.ItemIdentifier;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Log4j2
public class DuplicationChecker {

  private final Set<ItemIdentifier> identifiersToCheckDuplication = ConcurrentHashMap.newKeySet();
  private final Set<String> fetchedInstanceIds = ConcurrentHashMap.newKeySet();

  public boolean isDuplicate(ItemIdentifier itemIdentifier) {
    var res = !identifiersToCheckDuplication.add(itemIdentifier);
    if (res) {
      log.info("identifiersToCheckDuplication {}", identifiersToCheckDuplication);
    }
    return res;
  }

  public boolean fetched(String id) {
    return !fetchedInstanceIds.add(id);
  }

  public void reset() {
    identifiersToCheckDuplication.clear();
    fetchedInstanceIds.clear();
  }

  public Set<ItemIdentifier> getIdentifiersToCheckDuplication() {
    return identifiersToCheckDuplication;
  }

  @PostConstruct
  public void checkClassInstances() {
    log.info("DuplicationChecker this: {}", this);
  }
}
