package org.folio.dew.client;

import org.folio.dew.domain.dto.AccountdataCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "accounts")
public interface AccountClient {
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  AccountdataCollection getAccounts(@RequestParam String query, @RequestParam long limit);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  AccountdataCollection getAccounts(@RequestParam String query, @RequestParam long limit, @RequestParam long offset);
}
