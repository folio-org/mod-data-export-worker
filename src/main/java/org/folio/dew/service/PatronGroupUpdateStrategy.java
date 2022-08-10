package org.folio.dew.service;

import static java.lang.String.format;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.dew.domain.dto.UserContentUpdateAction.NameEnum.FIND;

import org.folio.dew.domain.dto.UserContentUpdate;
import org.folio.dew.domain.dto.UserContentUpdateAction;
import org.folio.dew.domain.dto.UserFormat;
import org.folio.dew.error.BulkEditException;

public class PatronGroupUpdateStrategy implements UpdateStrategy<UserFormat, UserContentUpdate> {
  @Override
  public UserFormat applyUpdate(UserFormat userFormat, UserContentUpdate update, boolean isPreview) {
    var firstAction = update.getActions().get(0);
    if (FIND == firstAction.getName()) {
      if (isEmpty(firstAction.getValue())) {
        throw new BulkEditException("Find value cannot be null or empty");
      } else if (userFormat.getPatronGroup().equals(firstAction.getValue())) {
        var nextAction = update.getActions().get(1);
        return performAction(userFormat, nextAction, isPreview);
      }
    } else {
      return performAction(userFormat, firstAction, isPreview);
    }
    return userFormat;
  }

  private UserFormat performAction(UserFormat userFormat, UserContentUpdateAction action, boolean isPreview) {
    switch (action.getName()) {
    case CLEAR_FIELD:
      if (isPreview) {
        return userFormat.withPatronGroup(EMPTY);
      }
      throw new BulkEditException("Patron group cannot be cleared");
    case REPLACE_WITH:
      if (isPreview) {
        return userFormat.withPatronGroup(isEmpty(action.getValue()) ? EMPTY : action.getValue().toString());
      } else {
        if (isEmpty(action.getValue())) {
          throw new BulkEditException("Patron group value cannot be empty");
        } else if (action.getValue().toString().equals(userFormat.getPatronGroup())) {
          throw new BulkEditException("Patron group: no value update needed");
        }
        return userFormat.withPatronGroup(action.getValue().toString());
      }
    default:
      throw new BulkEditException(format("%s cannot be applied to Patron group", action.getName()));
    }
  }
}
