package org.folio.dew.service;

import static java.lang.String.format;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.dew.domain.dto.UserContentUpdateAction.NameEnum.FIND;

import org.folio.dew.domain.dto.UserContentUpdate;
import org.folio.dew.domain.dto.UserContentUpdateAction;
import org.folio.dew.domain.dto.UserFormat;
import org.folio.dew.error.BulkEditException;

public class ExpirationDateUpdateStrategy implements UpdateStrategy<UserFormat, UserContentUpdate> {
  @Override
  public UserFormat applyUpdate(UserFormat userFormat, UserContentUpdate update, boolean isPreview) {
    var firstAction = update.getActions().get(0);
    if (FIND == firstAction.getName()) {
      if (isEmpty(firstAction.getValue())) {
        throw new BulkEditException("Find value cannot be null or empty");
      } else if (userFormat.getExpirationDate().equals(firstAction.getValue())) {
        var nextAction = update.getActions().get(1);
        return performAction(userFormat, nextAction, isPreview);
      }
    } else {
      return performAction(userFormat, firstAction, isPreview);
    }
    return userFormat;
  }

  private UserFormat performAction(UserFormat userFormat, UserContentUpdateAction action, boolean isPreview) {
    var newValue = isEmpty(action.getValue()) ? EMPTY : action.getValue().toString();
    if (!isPreview && newValue.equals(userFormat.getExpirationDate())) {
      throw new BulkEditException("Expiration date: No change in value needed");
    }
    switch (action.getName()) {
    case CLEAR_FIELD:
      return userFormat.withExpirationDate(EMPTY);
    case REPLACE_WITH:
      return userFormat.withExpirationDate(newValue);
    default:
      throw new BulkEditException(format("%s cannot be applied to Expiration date", action.getName()));
    }
  }
}
