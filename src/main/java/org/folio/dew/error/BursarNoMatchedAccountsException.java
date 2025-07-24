package org.folio.dew.error;

public class BursarNoMatchedAccountsException extends IllegalStateException {

  public BursarNoMatchedAccountsException() {
    super("There are fee/fine accounts with a balance, however, all were filtered out by the configured criteria.");
  }
}
