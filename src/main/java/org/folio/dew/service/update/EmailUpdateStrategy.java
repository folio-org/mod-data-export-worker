package org.folio.dew.service.update;

import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.UserContentUpdate;
import org.folio.dew.domain.dto.UserFormat;
import org.folio.dew.error.BulkEditException;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Log4j2
public class EmailUpdateStrategy implements UpdateStrategy<UserFormat, UserContentUpdate> {
  @Override
  public UserFormat applyUpdate(UserFormat userFormat, UserContentUpdate update) {
    var email = new Email(userFormat.getEmail());
    var findValue = update.getActions().get(0).getValue().toString();
    var replaceWithValue = update.getActions().get(1).getValue().toString();
    if (email.domainMatch(findValue)) {
      return userFormat.withEmail(email.asStringWithDomain(findValue, replaceWithValue));
    }
    throw new BulkEditException("Email does not match find criteria");
  }

  static class Email {
    private final String localPart;
    private final String domain;
    private final String [] domainTokens;

    public Email(String emailString) {
      var tokens = emailString.split("@");
      if (tokens.length != 2) {
        String msg = "Invalid email address: " + emailString;
        log.error(msg);
        throw new BulkEditException(msg);
      }
      localPart = tokens[0];
      domain = tokens[1];
      domainTokens = domain.split("[.]");
    }

    public boolean domainMatch(String value) {
      return domain.equals(value) || Arrays.stream(domainTokens).anyMatch(t -> t.equals(value));
    }

    public String asStringWithDomain(String findValue, String replaceWithValue) {
      return String.join("@", localPart, domain.replace(findValue, replaceWithValue));
    }
  }
}
