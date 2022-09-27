package org.folio.dew.error;

public class FileOperationException extends RuntimeException {
  public FileOperationException(String message) {
    super(message);
  }

  public FileOperationException(String message, Exception e) {
    super(message, e);
  }

  public FileOperationException(Exception e) {
    super(e);
  }
}
