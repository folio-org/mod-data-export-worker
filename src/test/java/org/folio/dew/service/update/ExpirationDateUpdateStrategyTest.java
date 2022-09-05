package org.folio.dew.service.update;

import static org.folio.dew.utils.BulkEditProcessorHelper.dateToString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.folio.dew.domain.dto.UserContentUpdate;
import org.folio.dew.domain.dto.UserContentUpdateAction;
import org.folio.dew.domain.dto.UserFormat;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Date;

class ExpirationDateUpdateStrategyTest {
  private final static String NEW_VALUE = dateToString(new Date());
  private final ExpirationDateUpdateStrategy updateStrategy = new ExpirationDateUpdateStrategy();

  @Test
  void shouldUpdateExpirationDate() {
    var userFormat = new UserFormat().withExpirationDate("2022-01-01 12:00:00.123Z");
    var contentUpdate = new UserContentUpdate()
      .option(UserContentUpdate.OptionEnum.EXPIRATION_DATE)
      .actions(Collections.singletonList(new UserContentUpdateAction()
        .name(UserContentUpdateAction.NameEnum.REPLACE_WITH)
        .value(NEW_VALUE)));

    var updatedUserFormat = updateStrategy.applyUpdate(userFormat, contentUpdate);

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

    assertThat(userFormat, equalTo(updateStrategy.applyUpdate(userFormat, contentUpdate)));
  }
}
