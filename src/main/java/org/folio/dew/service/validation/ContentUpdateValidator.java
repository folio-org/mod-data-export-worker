package org.folio.dew.service.validation;

public interface ContentUpdateValidator<T> {
  boolean isValid(T update);
}
