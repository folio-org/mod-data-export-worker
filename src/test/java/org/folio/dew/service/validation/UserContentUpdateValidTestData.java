package org.folio.dew.service.validation;

import static org.folio.dew.domain.dto.UserContentUpdate.OptionEnum.EMAIL_ADDRESS;
import static org.folio.dew.domain.dto.UserContentUpdate.OptionEnum.EXPIRATION_DATE;
import static org.folio.dew.domain.dto.UserContentUpdate.OptionEnum.PATRON_GROUP;
import static org.folio.dew.domain.dto.UserContentUpdateAction.NameEnum.CLEAR_FIELD;
import static org.folio.dew.domain.dto.UserContentUpdateAction.NameEnum.FIND;
import static org.folio.dew.domain.dto.UserContentUpdateAction.NameEnum.REPLACE_WITH;

import org.folio.dew.domain.dto.UserContentUpdate;
import org.folio.dew.domain.dto.UserContentUpdateAction;

import java.util.Collections;
import java.util.List;

public enum UserContentUpdateValidTestData {
  EXPIRATION_DATE_CLEAR_FIELD(
    new UserContentUpdate()
      .option(EXPIRATION_DATE)
      .actions(Collections.singletonList(
        new UserContentUpdateAction()
          .name(CLEAR_FIELD)))),
  EXPIRATION_DATE_REPLACE_WITH(
    new UserContentUpdate()
      .option(EXPIRATION_DATE)
      .actions(Collections.singletonList(
      new UserContentUpdateAction()
        .name(REPLACE_WITH)
        .value("new value")))),
  EXPIRATION_DATE_REPLACE_WITH_EMPTY_VALUE(
    new UserContentUpdate()
      .option(EXPIRATION_DATE)
      .actions(Collections.singletonList(
      new UserContentUpdateAction()
        .name(REPLACE_WITH)))),
  PATRON_GROUP_REPLACE_WITH(
    new UserContentUpdate()
      .option(PATRON_GROUP)
      .actions(Collections.singletonList(
      new UserContentUpdateAction()
        .name(REPLACE_WITH)
        .value("new value")))),
  EMAIL_FIND_AND_REPLACE_WITH(
    new UserContentUpdate()
    .option(EMAIL_ADDRESS)
    .actions(List.of(
      new UserContentUpdateAction()
        .name(FIND)
        .value("find value"),
      new UserContentUpdateAction()
        .name(REPLACE_WITH)
        .value("new value"))));

  private UserContentUpdate update;

  UserContentUpdateValidTestData(UserContentUpdate update) {
    this.update = update;
  }

  public UserContentUpdate getUpdate() {
    return update;
  }
}
