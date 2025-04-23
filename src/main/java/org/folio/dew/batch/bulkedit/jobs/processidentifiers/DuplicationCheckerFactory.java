package org.folio.dew.batch.bulkedit.jobs.processidentifiers;

import org.folio.dew.domain.dto.ItemIdentifier;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Component
public class DuplicationCheckerFactory {

  @SuppressWarnings("unchecked")
  public Set<ItemIdentifier> getIdentifiersToCheckDuplication(JobExecution jobExecution) {
    final String key = "identifiersToCheckDuplication";
    ExecutionContext context = jobExecution.getExecutionContext();

    synchronized (context) {
      if (!context.containsKey(key)) {
        context.put(key, Collections.synchronizedSet(new HashSet<>()));
      }
      return (Set<ItemIdentifier>) context.get(key);
    }
  }

  @SuppressWarnings("unchecked")
  public Set<String> getFetchedIds(JobExecution jobExecution) {
    final String key = "fetchedIds";
    ExecutionContext context = jobExecution.getExecutionContext();

    synchronized (context) {
      if (!context.containsKey(key)) {
        context.put(key, Collections.synchronizedSet(new HashSet<>()));
      }
      return (Set<String>) context.get(key);
    }
  }
}
