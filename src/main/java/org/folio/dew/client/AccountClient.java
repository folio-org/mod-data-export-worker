package org.folio.dew.client;

import org.folio.dew.domain.dto.AccountdataCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "accounts")
public interface AccountClient {
  @GetExchange(accept = MediaType.APPLICATION_JSON_VALUE)
  AccountdataCollection getAccounts(@RequestParam String query, @RequestParam long limit);

  @GetExchange(accept = MediaType.APPLICATION_JSON_VALUE)
  AccountdataCollection getAccounts(@RequestParam String query, @RequestParam long limit, @RequestParam long offset);
}
