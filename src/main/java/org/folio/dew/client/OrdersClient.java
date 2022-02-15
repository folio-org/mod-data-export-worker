package org.folio.dew.client;

import org.folio.dew.config.feign.FeignClientConfiguration;
import org.folio.dew.domain.dto.CompositePurchaseOrder;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "orders", configuration = FeignClientConfiguration.class)
public interface OrdersClient {

  @GetMapping(value = "/composite-orders/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  CompositePurchaseOrder getCompositePurchaseOrderById(@PathVariable String id);

}
