package org.folio.dew.error;

public class BulkEditException extends RuntimeException {

  private final org.folio.dew.domain.dto.ErrorType errorType;
  public BulkEditException(String message, org.folio.dew.domain.dto.ErrorType errorType) {
    super(message.replace(',', '_'));
    this.errorType = errorType;
  }

  public org.folio.dew.domain.dto.ErrorType getErrorType() {
    return errorType;
  }
}
