package org.folio.dew.service.validation;

import static java.util.Objects.nonNull;
import static org.folio.dew.domain.dto.UserContentUpdateAction.NameEnum.CLEAR_FIELD;
import static org.folio.dew.domain.dto.UserContentUpdateAction.NameEnum.REPLACE_WITH;

import org.folio.dew.domain.dto.UserContentUpdate;
import org.folio.dew.error.ContentUpdateValidationException;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ExpirationDateUpdateValidator implements ContentUpdateValidator<UserContentUpdate> {
  @Override
  public boolean isValid(UserContentUpdate update) {
    String errorMessage = null;
    if (update.getActions().size() != 1) {
      errorMessage = "Expiration date update should consist of single CLEAR_FIELD or REPLACE_WITH action";
    } else {
      var action = update.getActions().get(0);
      if (!Set.of(REPLACE_WITH, CLEAR_FIELD).contains(action.getName())) {
        errorMessage = action.getName() + " cannot be applied to Expiration date";
      }
    }
    if (nonNull(errorMessage)) {
      throw new ContentUpdateValidationException(errorMessage);
    }
    return true;
  }
}
