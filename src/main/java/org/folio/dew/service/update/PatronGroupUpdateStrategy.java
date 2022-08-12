package org.folio.dew.service.update;

import org.folio.dew.domain.dto.UserContentUpdate;
import org.folio.dew.domain.dto.UserFormat;
import org.folio.dew.error.BulkEditException;

public class PatronGroupUpdateStrategy implements UpdateStrategy<UserFormat, UserContentUpdate> {
  @Override
  public UserFormat applyUpdate(UserFormat userFormat, UserContentUpdate update, boolean isPreview) {
    var newPatronGroup = update.getActions().get(0).getValue().toString();
    if (isPreview) {
      return userFormat.withPatronGroup(newPatronGroup);
    } else if (newPatronGroup.equals(userFormat.getPatronGroup())) {
      throw new BulkEditException("Patron group: no change in value needed");
    }
    return userFormat.withPatronGroup(newPatronGroup);
  }
}
