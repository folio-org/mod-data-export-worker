package org.folio.dew.batch.acquisitions.mapper;

import java.util.List;

import org.folio.dew.batch.acquisitions.mapper.converter.OrderCsvFields;
import org.folio.dew.domain.dto.CompositePurchaseOrder;
import org.folio.dew.domain.dto.Piece;
import org.folio.dew.domain.dto.acquisitions.edifact.OrderCsvEntry;

public class OrderCsvMapper extends AbstractCsvMapper<OrderCsvEntry> {

  public OrderCsvMapper() {
    super(OrderCsvFields.values());
  }

  @Override
  protected List<OrderCsvEntry> getEntries(List<CompositePurchaseOrder> compPOs, List<Piece> pieces) {
    return compPOs.stream()
      .flatMap(order -> order.getPoLines().stream()
        .map(poLine -> new OrderCsvEntry(poLine, order)))
      .toList();
  }

}
