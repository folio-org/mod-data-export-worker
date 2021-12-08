package org.folio.dew.batch.bulkedit.jobs.processidentifiers;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.ItemIdentifier;
import org.folio.dew.domain.dto.User;
import org.folio.dew.error.BulkEditException;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class UserFetcher implements ItemProcessor<ItemIdentifier, User> {
  private static final String BARCODE = "barcode==";
  private static final String USER_NOT_FOUND_ERROR = "No match found";

  private final UserClient userClient;

  private Set<ItemIdentifier> identifiersToCheckDuplication = new HashSet<>();

  @Override
  public User process(ItemIdentifier itemIdentifier) throws BulkEditException {
    if (identifiersToCheckDuplication.contains(itemIdentifier)) {
      throw new BulkEditException("Duplicate entry");
    }
    identifiersToCheckDuplication.add(itemIdentifier);
    try {
      var users = userClient.getUserByQuery(BARCODE + itemIdentifier.getItemId(), 1);
      if (!users.getUsers().isEmpty()) {
        return users.getUsers().get(0);
      }
    } catch (FeignException e) {
      // When user not found 404
    }
    log.error(USER_NOT_FOUND_ERROR);
    throw new BulkEditException(USER_NOT_FOUND_ERROR);
  }
}
