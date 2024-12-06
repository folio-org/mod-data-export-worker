package org.folio.dew.batch.acquisitions.edifact.exceptions;

public class EntitiesNotFoundException extends RuntimeException {

  private static final String EXCEPTION_MESSAGE = "Entities not found: %s";

  public EntitiesNotFoundException(Class<?> entityClass) {
    super(EXCEPTION_MESSAGE.formatted(entityClass.getSimpleName()), null, false, false);
  }

}
