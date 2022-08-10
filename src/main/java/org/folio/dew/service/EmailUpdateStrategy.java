package org.folio.dew.service;

import org.folio.dew.domain.dto.UserContentUpdate;
import org.folio.dew.domain.dto.UserFormat;
import org.folio.dew.error.BulkEditException;

public class EmailUpdateStrategy implements UpdateStrategy<UserFormat, UserContentUpdate> {
  @Override
  public UserFormat applyUpdate(UserFormat userFormat, UserContentUpdate update, boolean isPreview) {
    try {
      var email = new Email(userFormat.getEmail());
      var findValue = update.getActions().get(0).getValue().toString();
      var replaceWithValue = update.getActions().get(1).getValue().toString();
      if (email.domainMatch(findValue)) {
        return userFormat.withEmail(email.asStringWithDomain(replaceWithValue));
      }
      return userFormat;
    } catch (BulkEditException e) {
      if (isPreview) {
        return userFormat;
      } else {
        throw e;
      }
    }
  }

  static class Email {
    private final String localPart;
    private final String domain;

    public Email(String emailString) {
      var tokens = emailString.split("@");
      if (tokens.length != 2) {
        throw new BulkEditException(emailString + " is not valid email address");
      }
      localPart = tokens[0];
      domain = tokens[1];
    }

    public boolean domainMatch(String value) {
      return domain.equals(value);
    }

    public String asStringWithDomain(String value) {
      return String.join("@", localPart, value);
    }
  }
}
