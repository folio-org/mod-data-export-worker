package org.folio.dew.service.update;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.folio.dew.domain.dto.UserContentUpdate;
import org.folio.dew.domain.dto.UserContentUpdateAction;
import org.folio.dew.domain.dto.UserFormat;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.service.PatronGroupUpdateStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.List;

class PatronGroupUpdateStrategyTest {
  private final static String NEW_VALUE = "New patron group";
  private final PatronGroupUpdateStrategy updateStrategy = new PatronGroupUpdateStrategy();

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void shouldUpdatePatronGroup(boolean isPreview) {
    var userFormat = new UserFormat().withPatronGroup("Patron group");
    var contentUpdate = new UserContentUpdate()
      .option(UserContentUpdate.OptionEnum.PATRON_GROUP)
      .actions(Collections.singletonList(new UserContentUpdateAction()
        .name(UserContentUpdateAction.NameEnum.REPLACE_WITH)
        .value(NEW_VALUE)));

    var updatedUserFormat = updateStrategy.applyUpdate(userFormat, contentUpdate, isPreview);

    assertThat(updatedUserFormat.getPatronGroup(), equalTo(NEW_VALUE));
  }

  @Test
  void shouldNotUpdatePatronGroupIfValuesAreEqual() {
    var userFormat = new UserFormat().withPatronGroup(NEW_VALUE);
    var contentUpdate = new UserContentUpdate()
      .option(UserContentUpdate.OptionEnum.PATRON_GROUP)
      .actions(Collections.singletonList(new UserContentUpdateAction()
        .name(UserContentUpdateAction.NameEnum.REPLACE_WITH)
        .value(NEW_VALUE)));

    assertThrows(BulkEditException.class, () -> updateStrategy.applyUpdate(userFormat, contentUpdate, false));
  }

  @Test
  void shouldUpdatePatronGroupIfValuesAreEqualForPreview() {
    var userFormat = new UserFormat().withPatronGroup(NEW_VALUE);
    var contentUpdate = new UserContentUpdate()
      .option(UserContentUpdate.OptionEnum.PATRON_GROUP)
      .actions(Collections.singletonList(new UserContentUpdateAction()
        .name(UserContentUpdateAction.NameEnum.REPLACE_WITH)
        .value(NEW_VALUE)));

    var updatedUserFormat = updateStrategy.applyUpdate(userFormat, contentUpdate, true);

    assertThat(updatedUserFormat.getPatronGroup(), equalTo(NEW_VALUE));
  }

  @ParameterizedTest
  @EnumSource(value = UserContentUpdateAction.NameEnum.class, names = {"CLEAR_FIELD", "REPLACE_WITH"}, mode = EnumSource.Mode.INCLUDE)
  void shouldNotClearPatronGroup(UserContentUpdateAction.NameEnum action) {
    var userFormat = new UserFormat().withPatronGroup("Patron group");
    var contentUpdate = new UserContentUpdate()
      .option(UserContentUpdate.OptionEnum.PATRON_GROUP)
      .actions(Collections.singletonList(new UserContentUpdateAction()
        .name(action)
        .value(null)));

    assertThrows(BulkEditException.class, () -> updateStrategy.applyUpdate(userFormat, contentUpdate, false));
  }

  @ParameterizedTest
  @EnumSource(value = UserContentUpdateAction.NameEnum.class, names = {"CLEAR_FIELD", "REPLACE_WITH"}, mode = EnumSource.Mode.INCLUDE)
  void shouldClearPatronGroupForPreview(UserContentUpdateAction.NameEnum action) {
    var userFormat = new UserFormat().withPatronGroup("Patron group");
    var contentUpdate = new UserContentUpdate()
      .option(UserContentUpdate.OptionEnum.PATRON_GROUP)
      .actions(Collections.singletonList(new UserContentUpdateAction()
        .name(action)
        .value(null)));

    var updatedUserFormat = updateStrategy.applyUpdate(userFormat, contentUpdate, true);

    assertThat(updatedUserFormat.getPatronGroup(), equalTo(EMPTY));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void shouldUpdatePatronGroupIfFindMatch(boolean isPreview) {
    var userFormat = new UserFormat().withPatronGroup("Patron group");
    var contentUpdate = new UserContentUpdate()
      .option(UserContentUpdate.OptionEnum.PATRON_GROUP)
      .actions(List.of(
        new UserContentUpdateAction()
          .name(UserContentUpdateAction.NameEnum.FIND)
          .value("Patron group"),
        new UserContentUpdateAction()
          .name(UserContentUpdateAction.NameEnum.REPLACE_WITH)
          .value(NEW_VALUE)));

    var updatedUserFormat = updateStrategy.applyUpdate(userFormat, contentUpdate, isPreview);

    assertThat(updatedUserFormat.getPatronGroup(), equalTo(NEW_VALUE));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void shouldNotUpdatePatronGroupIfFindDoesNotMatch(boolean isPreview) {
    var userFormat = new UserFormat().withPatronGroup("Patron group");
    var contentUpdate = new UserContentUpdate()
      .option(UserContentUpdate.OptionEnum.PATRON_GROUP)
      .actions(List.of(
        new UserContentUpdateAction()
          .name(UserContentUpdateAction.NameEnum.FIND)
          .value("Other group"),
        new UserContentUpdateAction()
          .name(UserContentUpdateAction.NameEnum.REPLACE_WITH)
          .value(NEW_VALUE)));

    var updatedUserFormat = updateStrategy.applyUpdate(userFormat, contentUpdate, isPreview);

    assertThat(updatedUserFormat.getPatronGroup(), equalTo("Patron group"));
  }

  @ParameterizedTest
  @EnumSource(value = UserContentUpdateAction.NameEnum.class, names = {"ADD_TO_EXISTING", "FIND_AND_REMOVE_THESE"}, mode = EnumSource.Mode.INCLUDE)
  void shouldNotApplyUnsupportedAction(UserContentUpdateAction.NameEnum action) {
    var userFormat = new UserFormat().withPatronGroup("Patron group");
    var contentUpdate = new UserContentUpdate()
      .option(UserContentUpdate.OptionEnum.PATRON_GROUP)
      .actions(Collections.singletonList(new UserContentUpdateAction()
        .name(action)
        .value(NEW_VALUE)));

    assertThrows(BulkEditException.class, () -> updateStrategy.applyUpdate(userFormat, contentUpdate, false));
  }
}
