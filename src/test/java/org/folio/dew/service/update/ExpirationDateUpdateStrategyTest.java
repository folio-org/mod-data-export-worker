package org.folio.dew.service.update;

import static java.time.ZoneOffset.UTC;
import static org.folio.dew.utils.BulkEditProcessorHelper.dateToString;
import static org.folio.dew.utils.Constants.DATE_TIME_PATTERN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.folio.dew.domain.dto.UserContentUpdate;
import org.folio.dew.domain.dto.UserContentUpdateAction;
import org.folio.dew.domain.dto.UserFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;

class ExpirationDateUpdateStrategyTest {
  private final static String CURRENT_DATE = dateToString(new Date());
  private final ExpirationDateUpdateStrategy updateStrategy = new ExpirationDateUpdateStrategy();

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void shouldUpdateExpirationDateAndActiveValue(boolean isActive) {
    var userFormat = new UserFormat().withExpirationDate(CURRENT_DATE);
    var newDate = isActive ?
        dateToString(Date.from(LocalDateTime.now().plusDays(1).atZone(UTC).toInstant())) :
        dateToString(Date.from(LocalDateTime.now().minusDays(1).atZone(UTC).toInstant()));
    var contentUpdate = new UserContentUpdate()
      .option(UserContentUpdate.OptionEnum.EXPIRATION_DATE)
      .actions(Collections.singletonList(new UserContentUpdateAction()
        .name(UserContentUpdateAction.NameEnum.REPLACE_WITH)
        .value(newDate)));
    var updatedUserFormat = updateStrategy.applyUpdate(userFormat, contentUpdate);

    assertThat(updatedUserFormat.getExpirationDate(), equalTo(newDate));
    assertThat(updatedUserFormat.getActive(), equalTo(Boolean.toString(isActive)));
  }

  @Test
  void shouldNotUpdateExpirationDateIfValuesAreEqual() {
    var userFormat = new UserFormat().withExpirationDate(CURRENT_DATE);
    var contentUpdate = new UserContentUpdate()
      .option(UserContentUpdate.OptionEnum.EXPIRATION_DATE)
      .actions(Collections.singletonList(new UserContentUpdateAction()
        .name(UserContentUpdateAction.NameEnum.REPLACE_WITH)
        .value(CURRENT_DATE)));

    assertThat(userFormat, equalTo(updateStrategy.applyUpdate(userFormat, contentUpdate)));
  }
}
