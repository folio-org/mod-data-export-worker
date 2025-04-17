package org.folio.dew.batch.bulkedit.jobs.processidentifiers;

import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.ItemIdentifier;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@JobScope
@Component
@Log4j2
public class DuplicationChecker {

  private final Set<ItemIdentifier> identifiersToCheckDuplication = ConcurrentHashMap.newKeySet();
  private final Set<String> fetchedIds = ConcurrentHashMap.newKeySet();

  public boolean isDuplicate(ItemIdentifier itemIdentifier) {
    return !identifiersToCheckDuplication.add(itemIdentifier);
  }

  public boolean wasNotFetched(String id) {
    return fetchedIds.add(id);
  }

  public void addAll(List<String> fetchedIds) {
    fetchedIds.addAll(fetchedIds);
  }

  @PostConstruct
  public void initialize() {
    log.info("DuplicationChecker initialized: {}, size: {}", this, identifiersToCheckDuplication.size());
  }
}
