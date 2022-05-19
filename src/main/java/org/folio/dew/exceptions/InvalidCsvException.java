package org.folio.dew.exceptions;

public class InvalidCsvException extends RuntimeException {
  public InvalidCsvException(String message) {
    super(message);
  }
}
