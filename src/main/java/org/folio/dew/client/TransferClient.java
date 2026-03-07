package org.folio.dew.client;

import org.folio.dew.domain.dto.TransferdataCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "transfers", accept = MediaType.APPLICATION_JSON_VALUE)
public interface TransferClient {

  @GetExchange
  TransferdataCollection get(@RequestParam String query, @RequestParam long limit);

}
