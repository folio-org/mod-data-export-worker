package org.folio.dew.error;

public class MarcValidationException extends RuntimeException {
  public MarcValidationException(String message) {
    super(message);
  }
}
