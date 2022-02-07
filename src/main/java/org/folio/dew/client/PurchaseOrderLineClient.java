package org.folio.dew.client;

import org.folio.dew.domain.dto.CompositePoLine;
import org.folio.dew.domain.dto.PoLineCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "orders/order-lines")
public interface PurchaseOrderLineClient {

  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  CompositePoLine getCompositePoLineById(@PathVariable String id);


  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  PoLineCollection getPoLineByQuery(@RequestParam("query") String query);

}
