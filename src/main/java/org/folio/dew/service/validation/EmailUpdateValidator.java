package org.folio.dew.service.validation;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.folio.dew.domain.dto.UserContentUpdateAction.NameEnum.FIND;
import static org.folio.dew.domain.dto.UserContentUpdateAction.NameEnum.REPLACE_WITH;

import org.folio.dew.domain.dto.UserContentUpdate;
import org.folio.dew.error.ContentUpdateValidationException;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class EmailUpdateValidator implements ContentUpdateValidator<UserContentUpdate> {
  @Override
  public boolean isValid(UserContentUpdate update) {
    String errorMessage = null;
    if (update.getActions().size() == 2) {
      var findAction = update.getActions().get(0);
      var replaceWithAction = update.getActions().get(1);
      if (FIND != findAction.getName() || REPLACE_WITH != replaceWithAction.getName()) {
        errorMessage = "Email update must contain FIND action followed by REPLACE_WITH action";
      } else if (isEmpty(findAction.getValue()) || isEmpty(replaceWithAction.getValue())) {
        errorMessage = "FIND and REPLACE_WITH values cannot be null or empty";
      } else if (Objects.equals(findAction.getValue(), replaceWithAction.getValue())) {
        errorMessage = "FIND and REPLACE_WITH values cannot be equal";
      }
    } else {
      errorMessage = "Email update must contain FIND action followed by REPLACE_WITH action";
    }
    if (nonNull(errorMessage)) {
      throw new ContentUpdateValidationException(errorMessage);
    }
    return true;
  }
}
