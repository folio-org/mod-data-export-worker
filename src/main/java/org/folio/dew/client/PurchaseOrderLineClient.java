package org.folio.dew.client;

import org.folio.dew.domain.dto.CompositePoLine;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "orders/order-lines")
public interface PurchaseOrderLineClient {

  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  CompositePoLine getCompositePurchaseCompositePoLineById(@PathVariable String id);



}
