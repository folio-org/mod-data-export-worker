package org.folio.dew.service.update;

import org.folio.dew.domain.dto.UserContentUpdate;
import org.folio.dew.domain.dto.UserFormat;
import org.springframework.stereotype.Component;

@Component
public class ExpirationDateUpdateStrategy implements UpdateStrategy<UserFormat, UserContentUpdate> {
  @Override
  public UserFormat applyUpdate(UserFormat userFormat, UserContentUpdate update) {
    return userFormat.withExpirationDate(update.getActions().get(0).getValue().toString());
  }
}
