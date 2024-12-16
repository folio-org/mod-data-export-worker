package org.folio.dew.batch.acquisitions.edifact.mapper.converter;

import java.util.Arrays;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.folio.dew.batch.acquisitions.edifact.services.OrdersService;
import org.folio.dew.domain.dto.CompositePoLine;
import org.folio.dew.domain.dto.Piece;

public class ClaimCsvConverter extends AbstractCsvConverter<Pair<CompositePoLine, Piece>> {

  private final OrdersService ordersService;

  public ClaimCsvConverter(OrdersService ordersService) {
    this.ordersService = ordersService;
  }

  @Override
  protected ExtractableField<Pair<CompositePoLine, Piece>, String>[] getFields() {
    return ClaimCsvFields.values();
  }

  @Override
  protected String[] getHeaders() {
    return Arrays.stream(ClaimCsvFields.values())
      .map(ClaimCsvFields::getName)
      .toArray(String[]::new);
  }

  @Override
  protected String extractField(Pair<CompositePoLine, Piece> entry, ExtractableField<Pair<CompositePoLine, Piece>, String> field) {
    return Optional.ofNullable(super.extractField(entry, field))
      .orElseGet(() -> {
        if (field == ClaimCsvFields.TITLE) {
          return ordersService.getTitleById(entry.getValue().getTitleId()).getTitle();
        }
        return null;
      });
  }

}
