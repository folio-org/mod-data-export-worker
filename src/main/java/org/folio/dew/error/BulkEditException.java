package org.folio.dew.error;

public class BulkEditException extends RuntimeException {
  public BulkEditException(String message) {
    super(message.replace(',', '_'));
  }
}
