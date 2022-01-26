package org.folio.dew.client;

import org.folio.dew.domain.dto.CompositePurchaseOrder;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "orders/purchase-orders")
public interface PurchaseOrderClient {

  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  CompositePurchaseOrder getCompositePurchaseOrderById(@PathVariable String id);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  CompositePurchaseOrder getCompositePurchaseOrderByQuery(@RequestParam String query);

}
