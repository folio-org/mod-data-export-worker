package org.folio.dew.error;

public class NotFoundException extends RuntimeException {

  private static final String EXCEPTION_MESSAGE = "Entities not found: %s";

  public NotFoundException(Class<?> entityClass) {
    super(EXCEPTION_MESSAGE.formatted(entityClass.getSimpleName()), null, false, false);
  }

  public NotFoundException(String message) {
    super(message);
  }

  public NotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

}
