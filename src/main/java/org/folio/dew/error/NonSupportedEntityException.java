package org.folio.dew.error;

public class NonSupportedEntityException extends RuntimeException {
  public NonSupportedEntityException(String message) {
    super(message);
  }
}
