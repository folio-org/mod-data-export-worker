package org.folio.dew.service.update;

import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.UserContentUpdate;
import org.folio.dew.domain.dto.UserFormat;
import org.folio.dew.error.BulkEditException;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Log4j2
public class EmailUpdateStrategy implements UpdateStrategy<UserFormat, UserContentUpdate> {
  @Override
  public UserFormat applyUpdate(UserFormat userFormat, UserContentUpdate update) {
    var findValue = update.getActions().get(0).getValue().toString();
    var replaceWithValue = update.getActions().get(1).getValue().toString();
    if (userFormat.getEmail().contains(findValue)) {
      return userFormat.withEmail(userFormat.getEmail().replace(findValue, replaceWithValue));
    }
    throw new BulkEditException("Email does not match find criteria");
  }
}
