package org.folio.dew.error;

public enum ErrorCode {
  UNKNOWN_ERROR("Unknown error"),
  VALIDATION_ERROR("Validation error"),
  NOT_FOUND_ERROR("Not found"),
  IO_ERROR("I/O error");

  private final String description;

  ErrorCode(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
