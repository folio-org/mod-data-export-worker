package org.folio.dew.service.update;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.folio.dew.domain.dto.UserContentUpdate;
import org.folio.dew.domain.dto.UserContentUpdateAction;
import org.folio.dew.domain.dto.UserFormat;
import org.junit.jupiter.api.Test;

import java.util.Collections;

class PatronGroupUpdateStrategyTest {
  private final static String NEW_VALUE = "New patron group";
  private final PatronGroupUpdateStrategy updateStrategy = new PatronGroupUpdateStrategy();

  @Test
  void shouldUpdatePatronGroup() {
    var userFormat = new UserFormat().withPatronGroup("Patron group");
    var contentUpdate = new UserContentUpdate()
      .option(UserContentUpdate.OptionEnum.PATRON_GROUP)
      .actions(Collections.singletonList(new UserContentUpdateAction()
        .name(UserContentUpdateAction.NameEnum.REPLACE_WITH)
        .value(NEW_VALUE)));

    var updatedUserFormat = updateStrategy.applyUpdate(userFormat, contentUpdate);

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

    assertThat(userFormat, equalTo(updateStrategy.applyUpdate(userFormat, contentUpdate)));
  }
}
