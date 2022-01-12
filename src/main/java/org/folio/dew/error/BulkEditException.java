package org.folio.dew.error;

public class BulkEditException extends Exception {
  public BulkEditException(String message) {
    super(message.replace(',', '_'));
  }
}
