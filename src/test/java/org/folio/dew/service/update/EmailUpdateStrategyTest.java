package org.folio.dew.service.update;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.folio.dew.domain.dto.UserContentUpdate;
import org.folio.dew.domain.dto.UserContentUpdateAction;
import org.folio.dew.domain.dto.UserFormat;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.service.EmailUpdateStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

class EmailUpdateStrategyTest {
  private final EmailUpdateStrategy updateStrategy = new EmailUpdateStrategy();

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void shouldUpdateEmailIfDomainMatch(boolean isPreview) {
    var userFormat = new UserFormat().withEmail("test@somedomain.com");
    var contentUpdate = new UserContentUpdate()
      .option(UserContentUpdate.OptionEnum.EMAIL_ADDRESS)
      .actions(List.of(new UserContentUpdateAction()
        .name(UserContentUpdateAction.NameEnum.FIND)
        .value("somedomain.com"),
        new UserContentUpdateAction()
          .name(UserContentUpdateAction.NameEnum.REPLACE_WITH)
          .value("newdomain.com")
        ));

    var updatedUserFormat = updateStrategy.applyUpdate(userFormat, contentUpdate, isPreview);

    assertThat(updatedUserFormat.getEmail(), equalTo("test@newdomain.com"));
  }

  @ParameterizedTest
  @CsvSource({"true,another.com", "true,SomeDomain.com", "false,another.com", "false,SomeDomain.com"})
  void shouldNotUpdateEmailIfDomainDoesNotMatch(String isPreview, String findValue) {
    var userFormat = new UserFormat().withEmail("test@somedomain.com");
    var contentUpdate = new UserContentUpdate()
      .option(UserContentUpdate.OptionEnum.EMAIL_ADDRESS)
      .actions(List.of(new UserContentUpdateAction()
          .name(UserContentUpdateAction.NameEnum.FIND)
          .value(findValue),
        new UserContentUpdateAction()
          .name(UserContentUpdateAction.NameEnum.REPLACE_WITH)
          .value("newdomain.com")
      ));

    var updatedUserFormat = updateStrategy.applyUpdate(userFormat, contentUpdate, Boolean.parseBoolean(isPreview));

    assertThat(updatedUserFormat.getEmail(), equalTo("test@somedomain.com"));
  }

  @Test
  void shouldNotUpdateEmailIfEmailIsNotValid() {
    var userFormat = new UserFormat().withEmail("test.somedomain.com");
    var contentUpdate = new UserContentUpdate()
      .option(UserContentUpdate.OptionEnum.EMAIL_ADDRESS)
      .actions(List.of(new UserContentUpdateAction()
          .name(UserContentUpdateAction.NameEnum.FIND)
          .value("somedomain.com"),
        new UserContentUpdateAction()
          .name(UserContentUpdateAction.NameEnum.REPLACE_WITH)
          .value("newdomain.com")
      ));

    assertThrows(BulkEditException.class, () -> updateStrategy.applyUpdate(userFormat, contentUpdate, false));
  }

  @Test
  void shouldReturnUnmodifiedUserFormatForPreviewIfEmailIsNotValid() {
    var userFormat = new UserFormat().withEmail("test.somedomain.com");
    var contentUpdate = new UserContentUpdate()
      .option(UserContentUpdate.OptionEnum.EMAIL_ADDRESS)
      .actions(List.of(new UserContentUpdateAction()
          .name(UserContentUpdateAction.NameEnum.FIND)
          .value("somedomain.com"),
        new UserContentUpdateAction()
          .name(UserContentUpdateAction.NameEnum.REPLACE_WITH)
          .value("newdomain.com")
      ));

    var updatedUserFormat = updateStrategy.applyUpdate(userFormat, contentUpdate, true);

    assertThat(updatedUserFormat.getEmail(), equalTo("test.somedomain.com"));
    }
}
