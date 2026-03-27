package org.folio.dew.client;

import org.folio.dew.domain.dto.ItemCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "inventory/items")
public interface InventoryClient {
  @GetExchange(accept = MediaType.APPLICATION_JSON_VALUE)
  ItemCollection getItemByQuery(@RequestParam("query") String query, @RequestParam long limit);

  @GetExchange(accept = MediaType.APPLICATION_JSON_VALUE)
  ItemCollection getItemByQuery(@RequestParam("query") String query, @RequestParam long offset, @RequestParam long limit);
}
