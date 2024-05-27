package org.folio.dew.domain.dto;

public interface Formatable<T> {
  T getOriginal();
  boolean isInstanceFormat();
  boolean isSourceMarc();
  String getId();
}
