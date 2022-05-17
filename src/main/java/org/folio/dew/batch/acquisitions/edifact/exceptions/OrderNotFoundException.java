package org.folio.dew.batch.acquisitions.edifact.exceptions;

public class OrderNotFoundException extends RuntimeException {

  public OrderNotFoundException(String message, boolean writableStackTrace) {
    super(message, null, false, writableStackTrace);
  }

}
