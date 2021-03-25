package org.folio.dew.client;

import org.folio.dew.domain.dto.TransferdataCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "transfers")
public interface TransferClient {

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  TransferdataCollection get(@RequestParam String query, @RequestParam long limit);

}
