package org.folio.dew.batch.acquisitions.mapper.converter;

public interface ExtractableField<T, R> {

  String getName();

  R extract(T item);

}
