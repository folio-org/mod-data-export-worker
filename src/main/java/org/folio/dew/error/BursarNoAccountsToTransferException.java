package org.folio.dew.error;

public class BursarNoAccountsToTransferException extends IllegalStateException {

  public BursarNoAccountsToTransferException() {
    super("No fee/fine accounts were found with a remaining balance that matched the provided criteria.");
  }
}
