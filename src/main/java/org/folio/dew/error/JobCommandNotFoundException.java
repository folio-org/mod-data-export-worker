package org.folio.dew.error;

public class JobCommandNotFoundException extends RuntimeException {
  public JobCommandNotFoundException(String message) {
    super(message);
  }
}
