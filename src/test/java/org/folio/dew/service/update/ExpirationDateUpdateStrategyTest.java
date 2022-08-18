package org.folio.dew.service.update;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.dew.utils.BulkEditProcessorHelper.dateToString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.folio.dew.domain.dto.UserContentUpdate;
import org.folio.dew.domain.dto.UserContentUpdateAction;
import org.folio.dew.domain.dto.UserFormat;
import org.folio.dew.error.BulkEditException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.Date;

class ExpirationDateUpdateStrategyTest {
  private final static String NEW_VALUE = dateToString(new Date());
  private final ExpirationDateUpdateStrategy updateStrategy = new ExpirationDateUpdateStrategy();

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void shouldUpdateExpirationDate(boolean isPreview) {
    var userFormat = new UserFormat().withExpirationDate("2022-01-01 12:00:00.123Z");
    var contentUpdate = new UserContentUpdate()
      .option(UserContentUpdate.OptionEnum.EXPIRATION_DATE)
      .actions(Collections.singletonList(new UserContentUpdateAction()
        .name(UserContentUpdateAction.NameEnum.REPLACE_WITH)
        .value(NEW_VALUE)));

    var updatedUserFormat = updateStrategy.applyUpdate(userFormat, contentUpdate, isPreview);

    assertThat(updatedUserFormat.getExpirationDate(), equalTo(NEW_VALUE));
  }

  @Test
  void shouldNotUpdateExpirationDateIfValuesAreEqual() {
    var userFormat = new UserFormat().withExpirationDate(NEW_VALUE);
    var contentUpdate = new UserContentUpdate()
      .option(UserContentUpdate.OptionEnum.EXPIRATION_DATE)
      .actions(Collections.singletonList(new UserContentUpdateAction()
        .name(UserContentUpdateAction.NameEnum.REPLACE_WITH)
        .value(NEW_VALUE)));

    assertThrows(BulkEditException.class, () -> updateStrategy.applyUpdate(userFormat, contentUpdate, false));
  }

  @Test
  void shouldUpdatePatronGroupIfValuesAreEqualForPreview() {
    var userFormat = new UserFormat().withExpirationDate(NEW_VALUE);
    var contentUpdate = new UserContentUpdate()
      .option(UserContentUpdate.OptionEnum.EXPIRATION_DATE)
      .actions(Collections.singletonList(new UserContentUpdateAction()
        .name(UserContentUpdateAction.NameEnum.REPLACE_WITH)
        .value(NEW_VALUE)));

    var updatedUserFormat = updateStrategy.applyUpdate(userFormat, contentUpdate, true);

    assertThat(updatedUserFormat.getExpirationDate(), equalTo(NEW_VALUE));
  }

  @ParameterizedTest
  @EnumSource(value = UserContentUpdateAction.NameEnum.class, names = {"CLEAR_FIELD", "REPLACE_WITH"}, mode = EnumSource.Mode.INCLUDE)
  void shouldClearExpirationDate(UserContentUpdateAction.NameEnum action) {
    var userFormat = new UserFormat().withExpirationDate("2022-01-01 12:00:00.123Z");
    var contentUpdate = new UserContentUpdate()
      .option(UserContentUpdate.OptionEnum.EXPIRATION_DATE)
      .actions(Collections.singletonList(new UserContentUpdateAction()
        .name(action)
        .value(null)));

    var updatedUserFormat = updateStrategy.applyUpdate(userFormat, contentUpdate, false);

    assertThat(updatedUserFormat.getExpirationDate(), equalTo(EMPTY));
  }

  @ParameterizedTest
  @EnumSource(value = UserContentUpdateAction.NameEnum.class, names = {"CLEAR_FIELD", "REPLACE_WITH"}, mode = EnumSource.Mode.INCLUDE)
  void shouldClearExpirationDateForPreview(UserContentUpdateAction.NameEnum action) {
    var userFormat = new UserFormat().withExpirationDate("2022-01-01 12:00:00.123Z");
    var contentUpdate = new UserContentUpdate()
      .option(UserContentUpdate.OptionEnum.EXPIRATION_DATE)
      .actions(Collections.singletonList(new UserContentUpdateAction()
        .name(action)
        .value(null)));

    var updatedUserFormat = updateStrategy.applyUpdate(userFormat, contentUpdate, true);

    assertThat(updatedUserFormat.getExpirationDate(), equalTo(EMPTY));
  }
}
