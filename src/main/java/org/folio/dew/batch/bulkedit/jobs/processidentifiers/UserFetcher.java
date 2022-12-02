package org.folio.dew.batch.bulkedit.jobs.processidentifiers;

import static org.folio.dew.utils.BulkEditProcessorHelper.resolveIdentifier;
import static org.folio.dew.utils.Constants.NO_MATCH_FOUND_MESSAGE;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.ItemIdentifier;
import org.folio.dew.domain.dto.User;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.error.NotFoundException;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class UserFetcher implements ItemProcessor<ItemIdentifier, User> {
  private final UserClient userClient;

  @Value("#{jobParameters['identifierType']}")
  private String identifierType;
  private Set<ItemIdentifier> identifiersToCheckDuplication = new HashSet<>();

  @Override
  public User process(ItemIdentifier itemIdentifier) throws BulkEditException {
    if (identifiersToCheckDuplication.contains(itemIdentifier)) {
      throw new BulkEditException("Duplicate entry");
    }
    identifiersToCheckDuplication.add(itemIdentifier);
    try {
      return userClient.getUserByQuery(String.format("%s==\"%s\"", resolveIdentifier(identifierType), itemIdentifier.getItemId()), 1).getUsers().get(0);
    } catch (NotFoundException e) {
      log.error(NO_MATCH_FOUND_MESSAGE);
      throw new BulkEditException(NO_MATCH_FOUND_MESSAGE);
    }
  }
}
