package org.folio.dew.batch.acquisitions.edifact.mapper.converter;

public interface ExtractableField<T, R> {

  String getName();

  R extract(T item);

}
