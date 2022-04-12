package org.folio.dew.error;

public class NonSupportedEntityTypeException extends RuntimeException {
  public NonSupportedEntityTypeException(String message) {
    super(message);
  }
}
