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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

class EmailUpdateStrategyTest {
  private final EmailUpdateStrategy updateStrategy = new EmailUpdateStrategy();

  @Test
  void shouldUpdateEmailIfDomainMatch() {
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

    var updatedUserFormat = updateStrategy.applyUpdate(userFormat, contentUpdate);

    assertThat(updatedUserFormat.getEmail(), equalTo("test@newdomain.com"));
  }

  @ParameterizedTest
  @CsvSource({"test,org@somedomain.com", "te,orgst@somedomain.com", "st,teorg@somedomain.com", "es,torgt@somedomain.com",
    "@,testorgsomedomain.com", "t@,tesorgsomedomain.com", "@s,testorgomedomain.com", "some,test@orgdomain.com",
    "med,test@soorgomain.com", "e,torgst@somorgdomain.com", ".com,test@somedomainorg", ".,test@somedomainorgcom",
    "o,test@sorgmedorgmain.corgm", "co,test@somedomain.orgm", "om,test@sorgedorgain.corg"})
  void shouldUpdateAnyPartOfEmail(String findValue, String expected) {
    var userFormat = new UserFormat().withEmail("test@somedomain.com");
    var contentUpdate = new UserContentUpdate()
      .option(UserContentUpdate.OptionEnum.EMAIL_ADDRESS)
      .actions(List.of(new UserContentUpdateAction()
          .name(UserContentUpdateAction.NameEnum.FIND)
          .value(findValue),
        new UserContentUpdateAction()
          .name(UserContentUpdateAction.NameEnum.REPLACE_WITH)
          .value("org")
      ));

    var updatedUserFormat = updateStrategy.applyUpdate(userFormat, contentUpdate);

    assertThat(updatedUserFormat.getEmail(), equalTo(expected));
  }

  @ParameterizedTest
  @ValueSource(strings = {"another.com", "SomeDomain.com"})
  void shouldThrowExceptionIfDomainDoesNotMatch(String findValue) {
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

    assertThrows(BulkEditException.class, () -> updateStrategy.applyUpdate(userFormat, contentUpdate));
  }
}
