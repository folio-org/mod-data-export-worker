package org.folio.dew.batch.acquisitions.mapper.converter;

import java.util.Arrays;

import org.folio.dew.domain.dto.acquisitions.edifact.OrderCsvEntry;

public class OrderCsvConverter extends AbstractCsvConverter<OrderCsvEntry> {

  @Override
  protected ExtractableField<OrderCsvEntry, String>[] getFields() {
    return OrderCsvFields.values();
  }

  @Override
  protected String[] getHeaders() {
    return Arrays.stream(OrderCsvFields.values())
      .map(OrderCsvFields::getName)
      .toArray(String[]::new);
  }

}
