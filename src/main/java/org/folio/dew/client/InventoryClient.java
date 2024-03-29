package org.folio.dew.client;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.folio.dew.config.feign.FeignEncoderConfiguration;
import org.folio.dew.domain.dto.Item;
import org.folio.dew.domain.dto.ItemCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "inventory/items", configuration = FeignEncoderConfiguration.class)
public interface InventoryClient {
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  ItemCollection getItemByQuery(@RequestParam String query);

  @GetMapping(value = "/{itemId}", produces = MediaType.APPLICATION_JSON_VALUE)
  Item getItemById(@PathVariable String itemId);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  ItemCollection getItemByQuery(@RequestParam("query") String query, @RequestParam long limit);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  ItemCollection getItemByQuery(@RequestParam("query") String query, @RequestParam long offset, @RequestParam long limit);

  @PutMapping(value = "/{itemId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  void updateItem(@RequestBody Item item, @PathVariable String itemId);
}
