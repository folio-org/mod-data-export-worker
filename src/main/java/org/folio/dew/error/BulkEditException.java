package org.folio.dew.error;

import org.folio.dew.domain.dto.ErrorType;

public class BulkEditException extends RuntimeException {

  private final org.folio.dew.domain.dto.ErrorType errorType;
  public BulkEditException(String message, org.folio.dew.domain.dto.ErrorType errorType) {
    super(message.replace(',', '_'));
    this.errorType = errorType;
  }

  public BulkEditException(String message) {
    super(message);
    errorType = ErrorType.ERROR;
  }

  public org.folio.dew.domain.dto.ErrorType getErrorType() {
    return errorType;
  }
}
