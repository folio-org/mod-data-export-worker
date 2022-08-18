package org.folio.dew.service.update;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import org.folio.dew.domain.dto.UserContentUpdate;
import org.folio.dew.domain.dto.UserFormat;
import org.folio.dew.error.BulkEditException;

public class ExpirationDateUpdateStrategy implements UpdateStrategy<UserFormat, UserContentUpdate> {
  @Override
  public UserFormat applyUpdate(UserFormat userFormat, UserContentUpdate update, boolean isPreview) {
    var action = update.getActions().get(0);
    var newExpirationDate = isEmpty(action.getValue()) ? EMPTY : action.getValue().toString();
    switch (action.getName()) {
    case REPLACE_WITH:
      if (!isPreview && newExpirationDate.equals(userFormat.getExpirationDate())) {
        throw new BulkEditException("Expiration date: No change in value needed");
      }
      return userFormat.withExpirationDate(newExpirationDate);
    case CLEAR_FIELD:
      if (!isPreview && EMPTY.equals(userFormat.getExpirationDate())) {
        throw new BulkEditException("Expiration date: No change in value needed");
      }
      return userFormat.withExpirationDate(EMPTY);
    default:
      return userFormat;
    }
  }
}
