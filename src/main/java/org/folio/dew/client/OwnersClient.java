package org.folio.dew.client;

import org.folio.dew.domain.dto.Ownerdatacollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "owners")
public interface OwnersClient {

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  Ownerdatacollection get(@RequestParam String query, @RequestParam long limit);

}
