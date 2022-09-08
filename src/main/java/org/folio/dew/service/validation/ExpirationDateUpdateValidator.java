package org.folio.dew.service.validation;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.folio.dew.domain.dto.UserContentUpdateAction.NameEnum.REPLACE_WITH;
import static org.folio.dew.utils.Constants.DATE_TIME_PATTERN;

import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.UserContentUpdate;
import org.folio.dew.error.ContentUpdateValidationException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
@Log4j2
public class ExpirationDateUpdateValidator implements ContentUpdateValidator<UserContentUpdate> {
  @Override
  public boolean isValid(UserContentUpdate update) {
    String errorMessage = null;
    var action = update.getActions().size() == 1 ? update.getActions().get(0) : null;
    if (isNull(action) || REPLACE_WITH != action.getName()) {
      errorMessage = "Expiration date update should consist of single REPLACE_WITH action";
    } else if (isEmpty(action.getValue())) {
      errorMessage = "Value cannot be empty";
    } else {
      try {
        LocalDateTime.parse(action.getValue().toString(), DateTimeFormatter.ofPattern(DATE_TIME_PATTERN));
      } catch (DateTimeParseException e) {
        errorMessage = String.format("Invalid date format: %s, expected yyyy-MM-dd HH:mm:ss.SSSX", action.getValue().toString());
        log.error(errorMessage);
      }
    }
    if (nonNull(errorMessage)) {
      throw new ContentUpdateValidationException(errorMessage);
    }
    return true;
  }
}
