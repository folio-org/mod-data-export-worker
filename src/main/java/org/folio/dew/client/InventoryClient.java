package org.folio.dew.client;

import org.folio.dew.config.feign.FeignClientConfiguration;
import org.folio.dew.config.feign.FeignEncoderConfiguration;
import org.folio.dew.domain.dto.ItemCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

  @FeignClient(name = "inventory/items", configuration = {FeignEncoderConfiguration.class, FeignClientConfiguration.class})
public interface InventoryClient {
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  ItemCollection getItemByQuery(@RequestParam("query") String query, @RequestParam long limit);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  ItemCollection getItemByQuery(@RequestParam("query") String query, @RequestParam long offset, @RequestParam long limit);
}
