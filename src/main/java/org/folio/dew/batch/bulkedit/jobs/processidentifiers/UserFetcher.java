package org.folio.dew.batch.bulkedit.jobs.processidentifiers;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static org.folio.dew.utils.BulkEditProcessorHelper.dateToString;
import static org.folio.dew.utils.BulkEditProcessorHelper.resolveIdentifier;
import static org.folio.dew.utils.Constants.MIN_YEAR_FOR_BIRTH_DATE;
import static org.folio.dew.utils.Constants.MULTIPLE_MATCHES_MESSAGE;
import static org.folio.dew.utils.Constants.NO_MATCH_FOUND_MESSAGE;
import static org.folio.dew.utils.Constants.NO_USER_VIEW_PERMISSIONS;

import feign.codec.DecodeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.batch.bulkedit.jobs.permissions.check.PermissionsValidator;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.EntityType;
import org.folio.dew.domain.dto.ErrorType;
import org.folio.dew.domain.dto.ItemIdentifier;
import org.folio.dew.domain.dto.User;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.utils.ExceptionHelper;
import org.folio.spring.FolioExecutionContext;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class UserFetcher implements ItemProcessor<ItemIdentifier, User> {
  private static final String USER_SEARCH_QUERY = "(cql.allRecords=1 NOT type=\"\" or type<>\"shadow\") and %s==\"%s\"";

  private final UserClient userClient;
  private final DuplicationChecker duplicationChecker;

  @Value("#{jobParameters['identifierType']}")
  private String identifierType;
  private final FolioExecutionContext folioExecutionContext;
  private final PermissionsValidator permissionsValidator;

  @Override
  public synchronized User process(ItemIdentifier itemIdentifier) throws BulkEditException {
    if (!permissionsValidator.isBulkEditReadPermissionExists(folioExecutionContext.getTenantId(), EntityType.USER)) {
      var user = userClient.getUserById(folioExecutionContext.getUserId().toString());
      throw new BulkEditException(format(NO_USER_VIEW_PERMISSIONS, user.getUsername(), resolveIdentifier(identifierType), itemIdentifier.getItemId(), folioExecutionContext.getTenantId()), ErrorType.ERROR);
    }
    if (duplicationChecker.isDuplicate(itemIdentifier)) {
      throw new BulkEditException("Duplicate entry", ErrorType.WARNING);
    }
    try {
      var limit = 1;
      var userCollection = userClient.getUserByQuery(
        String.format(USER_SEARCH_QUERY, resolveIdentifier(identifierType), itemIdentifier.getItemId()),
        limit
      );

      if (userCollection.getUsers().isEmpty()) {
        throw new BulkEditException(NO_MATCH_FOUND_MESSAGE, ErrorType.ERROR);
      } else if (userCollection.getTotalRecords() > limit) {
        throw new BulkEditException(MULTIPLE_MATCHES_MESSAGE, ErrorType.ERROR);
      }
      var user = userCollection.getUsers().get(0);
      var birthDate = user.getPersonal().getDateOfBirth();
      validateBirthDate(birthDate);
      return user;
    } catch (DecodeException e) {
      throw new BulkEditException(ExceptionHelper.fetchMessage(e), ErrorType.ERROR);
    }
  }

  private void validateBirthDate(Date birthDate) {
    if (nonNull(birthDate)) {
      var year = LocalDateTime.ofInstant(Instant.ofEpochMilli(birthDate.getTime()), ZoneOffset.UTC).getYear();
      if (year < MIN_YEAR_FOR_BIRTH_DATE) {
        throw new BulkEditException(String.format("Failed to parse Date from value \"%s\" in users.personal.dateOfBirth", dateToString(birthDate)), ErrorType.ERROR);
      }
    }
  }
}
