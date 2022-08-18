package org.folio.dew.service.update;

public interface UpdateStrategy<T,U> {
  T applyUpdate(T entity, U update, boolean isPreview);
}
