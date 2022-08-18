package org.folio.dew.service.update;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.folio.dew.domain.dto.UserContentUpdate;
import org.folio.dew.domain.dto.UserContentUpdateAction;
import org.folio.dew.domain.dto.UserFormat;
import org.folio.dew.error.BulkEditException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;

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
}
