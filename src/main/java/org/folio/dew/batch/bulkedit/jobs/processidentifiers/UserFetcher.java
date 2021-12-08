package org.folio.dew.batch.bulkedit.jobs.processidentifiers;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.ItemIdentifier;
import org.folio.dew.domain.dto.User;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class UserFetcher implements ItemProcessor<ItemIdentifier, User> {
  private static final String BARCODE = "barcode==";
  private static final String USER_NOT_FOUND_ERROR = "User with barcode=%s was not found";

  private final UserClient userClient;

  @Override
  public User process(ItemIdentifier itemIdentifier) {
    var users = userClient.getUserByQuery(BARCODE + itemIdentifier.getItemId(), 1);
    if (!users.getUsers().isEmpty()) {
      return users.getUsers().get(0);
    }
    var errorMessage = String.format(USER_NOT_FOUND_ERROR, itemIdentifier.getItemId());
    log.error(errorMessage);
    throw new IllegalArgumentException(errorMessage);
  }
}
