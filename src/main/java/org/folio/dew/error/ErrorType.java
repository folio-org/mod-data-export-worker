package org.folio.dew.error;

public enum ErrorType {
  INTERNAL("-1"),
  UNKNOWN("-4");

  private final String value;

  ErrorType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
