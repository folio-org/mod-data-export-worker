package org.folio.dew.service.validation;

import lombok.RequiredArgsConstructor;
import org.folio.dew.domain.dto.UserContentUpdate;
import org.folio.dew.domain.dto.UserContentUpdateCollection;
import org.folio.dew.error.ContentUpdateValidationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserContentUpdateValidatorService {
  private final ExpirationDateUpdateValidator expirationDateUpdateValidator;
  private final PatronGroupUpdateValidator patronGroupUpdateValidator;
  private final EmailUpdateValidator emailUpdateValidator;

  public boolean validateContentUpdateCollection(UserContentUpdateCollection contentUpdateCollection) {
    return contentUpdateCollection.getUserContentUpdates().stream()
      .allMatch(this::isValidContentUpdate);
  }

  private boolean isValidContentUpdate(UserContentUpdate update) {
    return resolveValidator(update).isValid(update);
  }

  private ContentUpdateValidator<UserContentUpdate> resolveValidator(UserContentUpdate update) {
    switch (update.getOption()) {
    case EXPIRATION_DATE:
      return expirationDateUpdateValidator;
    case PATRON_GROUP:
      return patronGroupUpdateValidator;
    case EMAIL_ADDRESS:
      return emailUpdateValidator;
    default:
      throw new ContentUpdateValidationException(update.getOption() + " update is not supported");
    }
  }
}
