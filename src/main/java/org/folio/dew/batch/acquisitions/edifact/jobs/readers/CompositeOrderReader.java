package org.folio.dew.batch.acquisitions.edifact.jobs.readers;

import org.folio.dew.batch.acquisitions.edifact.services.OrdersService;
import org.folio.dew.domain.dto.CompositePurchaseOrder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

public class CompositeOrderReader implements ItemReader<CompositePurchaseOrder> {
  OrdersService ordersService;
  @Override
  public CompositePurchaseOrder read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
    String query="";
    return ordersService.getCompositePurchaseOrderByQuery(query);
  }

/*  @Override
  protected List<LogRecord> getItems(int offset, int limit) {
    return ordersService.getCompositePurchaseOrderByQuery(query, offset, limit, null).getLogRecords();
  }*/
}
