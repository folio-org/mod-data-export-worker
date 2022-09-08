package org.folio.dew.service.validation;

import static org.folio.dew.domain.dto.UserContentUpdate.OptionEnum.EMAIL_ADDRESS;
import static org.folio.dew.domain.dto.UserContentUpdate.OptionEnum.EXPIRATION_DATE;
import static org.folio.dew.domain.dto.UserContentUpdate.OptionEnum.PATRON_GROUP;
import static org.folio.dew.domain.dto.UserContentUpdateAction.NameEnum.ADD_TO_EXISTING;
import static org.folio.dew.domain.dto.UserContentUpdateAction.NameEnum.CLEAR_FIELD;
import static org.folio.dew.domain.dto.UserContentUpdateAction.NameEnum.FIND;
import static org.folio.dew.domain.dto.UserContentUpdateAction.NameEnum.FIND_AND_REMOVE_THESE;
import static org.folio.dew.domain.dto.UserContentUpdateAction.NameEnum.REPLACE_WITH;

import org.folio.dew.domain.dto.UserContentUpdate;
import org.folio.dew.domain.dto.UserContentUpdateAction;

import java.util.Collections;
import java.util.List;

public enum UserContentUpdateInvalidTestData {
  PATRON_GROUP_REPLACE_WITH_EMPTY_VALUE(
    new UserContentUpdate()
      .option(PATRON_GROUP)
      .actions(Collections.singletonList(
      new UserContentUpdateAction()
        .name(REPLACE_WITH)))),
  PATRON_GROUP_REPLACE_WITH_NON_EXISTING_VALUE(
    new UserContentUpdate()
      .option(PATRON_GROUP)
      .actions(Collections.singletonList(
        new UserContentUpdateAction()
          .name(REPLACE_WITH)
          .value("non-existing group")
      ))
  ),
  PATRON_GROUP_CLEAR_FIELD(
    new UserContentUpdate()
      .option(PATRON_GROUP)
      .actions(Collections.singletonList(
        new UserContentUpdateAction()
          .name(CLEAR_FIELD)))),
  PATRON_GROUP_FIND(
    new UserContentUpdate()
      .option(PATRON_GROUP)
      .actions(Collections.singletonList(
        new UserContentUpdateAction()
          .name(FIND)
          .value("find value")))),
  PATRON_GROUP_FIND_AND_REPLACE_WITH(
    new UserContentUpdate()
      .option(PATRON_GROUP)
      .actions(List.of(
        new UserContentUpdateAction()
          .name(FIND)
          .value("find value"),
        new UserContentUpdateAction()
          .name(REPLACE_WITH)
          .value("new value")))),
  PATRON_GROUP_ADD_TO_EXISTING(
    new UserContentUpdate()
      .option(PATRON_GROUP)
      .actions(Collections.singletonList(
        new UserContentUpdateAction()
          .name(ADD_TO_EXISTING)
          .value("find value")))),
  PATRON_GROUP_FIND_AND_REMOVE_THESE(
    new UserContentUpdate()
      .option(PATRON_GROUP)
      .actions(Collections.singletonList(
        new UserContentUpdateAction()
          .name(FIND_AND_REMOVE_THESE)
          .value("find value")))),
  EXPIRATION_DATE_REPLACE_WITH_INVALID_DATE(
      new UserContentUpdate()
        .option(EXPIRATION_DATE)
        .actions(Collections.singletonList(
          new UserContentUpdateAction()
            .name(REPLACE_WITH)
            .value("2022/01/01")))),
  EXPIRATION_DATE_REPLACE_WITH_EMPTY_VALUE(
    new UserContentUpdate()
      .option(EXPIRATION_DATE)
      .actions(Collections.singletonList(
        new UserContentUpdateAction()
            .name(REPLACE_WITH)))),
  EXPIRATION_DATE_CLEAR_FIELD(
    new UserContentUpdate()
      .option(EXPIRATION_DATE)
      .actions(Collections.singletonList(
        new UserContentUpdateAction()
            .name(CLEAR_FIELD)))),
  EXPIRATION_DATE_FIND(
    new UserContentUpdate()
      .option(EXPIRATION_DATE)
      .actions(Collections.singletonList(
        new UserContentUpdateAction()
          .name(FIND)
          .value("find value")))),
  EXPIRATION_DATE_FIND_AND_REPLACE_WITH(
    new UserContentUpdate()
      .option(EXPIRATION_DATE)
      .actions(List.of(
        new UserContentUpdateAction()
          .name(FIND)
          .value("find value"),
        new UserContentUpdateAction()
          .name(REPLACE_WITH)
          .value("new value")))),
  EXPIRATION_DATE_ADD_TO_EXISTING(
    new UserContentUpdate()
      .option(EXPIRATION_DATE)
      .actions(Collections.singletonList(
        new UserContentUpdateAction()
          .name(ADD_TO_EXISTING)
          .value("find value")))),
  EXPIRATION_DATE_AND_REMOVE_THESE(
    new UserContentUpdate()
      .option(EXPIRATION_DATE)
      .actions(Collections.singletonList(
        new UserContentUpdateAction()
          .name(FIND_AND_REMOVE_THESE)
          .value("find value")))),
  EMAIL_REPLACE_WITH_EMPTY_VALUE(
    new UserContentUpdate()
      .option(EMAIL_ADDRESS)
      .actions(Collections.singletonList(
        new UserContentUpdateAction()
          .name(REPLACE_WITH)))),
  EMAIL_CLEAR_FIELD(
    new UserContentUpdate()
      .option(EMAIL_ADDRESS)
      .actions(Collections.singletonList(
        new UserContentUpdateAction()
          .name(CLEAR_FIELD)))),
  EMAIL_FIND(
    new UserContentUpdate()
      .option(EMAIL_ADDRESS)
      .actions(Collections.singletonList(
        new UserContentUpdateAction()
          .name(FIND)
          .value("find value")))),
  EMAIL_ADD_TO_EXISTING(
    new UserContentUpdate()
      .option(EMAIL_ADDRESS)
      .actions(Collections.singletonList(
        new UserContentUpdateAction()
          .name(ADD_TO_EXISTING)
          .value("find value")))),
  EMAIL_FIND_AND_REMOVE_THESE(
    new UserContentUpdate()
      .option(EMAIL_ADDRESS)
      .actions(Collections.singletonList(
        new UserContentUpdateAction()
          .name(FIND_AND_REMOVE_THESE)
          .value("find value")))),
  EMAIL_FIND_AND_AND_REPLACE_WITH_EMPTY_FIND(
    new UserContentUpdate()
      .option(EMAIL_ADDRESS)
      .actions(List.of(
        new UserContentUpdateAction()
          .name(FIND),
        new UserContentUpdateAction()
          .name(REPLACE_WITH)
          .value("replace value")))),
  EMAIL_FIND_AND_AND_REPLACE_WITH_EMPTY_REPLACE(
    new UserContentUpdate()
      .option(EMAIL_ADDRESS)
      .actions(List.of(
        new UserContentUpdateAction()
          .name(FIND)
          .value("find value"),
        new UserContentUpdateAction()
          .name(REPLACE_WITH)))),
  EMAIL_FIND_AND_AND_REPLACE_WITH_EQUAL_VALUES(
    new UserContentUpdate()
      .option(EMAIL_ADDRESS)
      .actions(List.of(
        new UserContentUpdateAction()
          .name(FIND)
          .value("some value"),
        new UserContentUpdateAction()
          .name(REPLACE_WITH)
          .value("some value"))));

  private UserContentUpdate update;

  UserContentUpdateInvalidTestData(UserContentUpdate update) {
    this.update = update;
  }

  public UserContentUpdate getUpdate() {
    return update;
  }
}
