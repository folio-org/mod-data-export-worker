package org.folio.dew.client;

import org.folio.dew.domain.dto.OrdersTitle;
import org.folio.dew.domain.dto.PieceCollection;
import org.folio.dew.domain.dto.PoLineCollection;
import org.folio.dew.domain.dto.PurchaseOrderCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "orders-storage", accept = MediaType.APPLICATION_JSON_VALUE)
public interface OrdersStorageClient {

  @GetExchange(value = "/purchase-orders")
  PurchaseOrderCollection getPurchaseOrdersByQuery(
    @RequestParam("query") String query,
    @RequestParam("offset") int offset,
    @RequestParam("limit") int limit
  );

  @GetExchange(value = "/po-lines")
  PoLineCollection getPoLinesByQuery(
    @RequestParam("query") String query,
    @RequestParam("offset") int offset,
    @RequestParam("limit") int limit
  );

  @GetExchange(value = "/pieces")
  PieceCollection getPiecesByQuery(
    @RequestParam("query") String query,
    @RequestParam("offset") int offset,
    @RequestParam("limit") int limit
  );

  @GetExchange(value = "/titles/{id}")
  OrdersTitle getTitleById(@PathVariable("id") String id);

}
