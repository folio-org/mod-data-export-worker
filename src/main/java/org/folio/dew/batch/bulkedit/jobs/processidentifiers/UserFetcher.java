package org.folio.dew.batch.bulkedit.jobs.processidentifiers;

import static java.lang.String.format;
import static org.folio.dew.utils.BulkEditProcessorHelper.resolveIdentifier;
import static org.folio.dew.utils.Constants.MULTIPLE_MATCHES_MESSAGE;
import static org.folio.dew.utils.Constants.NO_MATCH_FOUND_MESSAGE;
import static org.folio.dew.utils.Constants.NO_USER_VIEW_PERMISSIONS;

import feign.codec.DecodeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.batch.bulkedit.jobs.permissions.check.PermissionsValidator;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.EntityType;
import org.folio.dew.domain.dto.ItemIdentifier;
import org.folio.dew.domain.dto.User;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.utils.ExceptionHelper;
import org.folio.spring.FolioExecutionContext;
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
  private final FolioExecutionContext folioExecutionContext;
  private final PermissionsValidator permissionsValidator;

  @Override
  public User process(ItemIdentifier itemIdentifier) throws BulkEditException {
    if (!permissionsValidator.isBulkEditReadPermissionExists(folioExecutionContext.getTenantId(), EntityType.USER)) {
      var user = userClient.getUserById(folioExecutionContext.getUserId().toString());
      throw new BulkEditException(format(NO_USER_VIEW_PERMISSIONS, user.getUsername(), resolveIdentifier(identifierType), itemIdentifier.getItemId(), folioExecutionContext.getTenantId()));
    }
    if (identifiersToCheckDuplication.contains(itemIdentifier)) {
      throw new BulkEditException("Duplicate entry");
    }
    identifiersToCheckDuplication.add(itemIdentifier);
    try {
      var limit = 1;
      var userCollection = userClient.getUserByQuery(String.format("%s==\"%s\"", resolveIdentifier(identifierType), itemIdentifier.getItemId()), limit);
      if (userCollection.getUsers().isEmpty()) {
        throw new BulkEditException(NO_MATCH_FOUND_MESSAGE);
      } else if (userCollection.getTotalRecords() > limit) {
        throw new BulkEditException(MULTIPLE_MATCHES_MESSAGE);
      }
      return userCollection.getUsers().get(0);
    } catch (DecodeException e) {
      throw new BulkEditException(ExceptionHelper.fetchMessage(e));
    }
  }
}
