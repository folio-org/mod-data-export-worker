package org.folio.dew.error;

public class BursarNoAccountsException extends IllegalStateException {

  public BursarNoAccountsException() {
    super("No fee/fine accounts were found with a remaining balance.");
  }
}
