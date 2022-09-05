package org.folio.dew.service.update;

import static org.folio.dew.utils.BulkEditProcessorHelper.dateFromString;

import org.folio.dew.domain.dto.UserContentUpdate;
import org.folio.dew.domain.dto.UserFormat;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class ExpirationDateUpdateStrategy implements UpdateStrategy<UserFormat, UserContentUpdate> {
  @Override
  public UserFormat applyUpdate(UserFormat userFormat, UserContentUpdate update) {
    var action = update.getActions().get(0);
    return userFormat
      .withExpirationDate(action.getValue().toString())
      .withActive(Boolean.toString(new Date().before(dateFromString(action.getValue().toString()))));
  }
}
