package org.folio.dew.error;

public enum ErrorCode {
  UNKNOWN_ERROR("Unknown error"),
  VALIDATION_ERROR("Validation error"),
  NOT_FOUND_ERROR("Not found"),
  IO_ERROR("I/O error"),
  PROCESSING_ERROR("Processing error");

  private final String description;

  ErrorCode(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
