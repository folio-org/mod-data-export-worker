package org.folio.dew.client;

import org.folio.dew.domain.dto.PoLineCollection;
import org.folio.dew.domain.dto.PurchaseOrderCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "orders-storage")
public interface OrdersStorageClient {

  @GetMapping(value = "/purchase-orders", produces = MediaType.APPLICATION_JSON_VALUE)
  PurchaseOrderCollection getPurchaseOrdersByQuery(
    @RequestParam("query") String query,
    @RequestParam("offset") int offset,
    @RequestParam("limit") int limit
  );

  @GetMapping(value = "/po-lines", produces = MediaType.APPLICATION_JSON_VALUE)
  PoLineCollection getPoLinesByQuery(
    @RequestParam("query") String query,
    @RequestParam("offset") int offset,
    @RequestParam("limit") int limit
  );

}
