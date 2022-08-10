package org.folio.dew.service;

public interface UpdateStrategy<T,U> {
  T applyUpdate(T entity, U update, boolean isPreview);
}
