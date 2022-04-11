package org.folio.dew.error;

public class NonSupportedEntityType extends RuntimeException {
  public NonSupportedEntityType(String message) {
    super(message);
  }
}
