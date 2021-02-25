package org.folio.dew.client;

import org.folio.dew.domain.dto.FeefineactionCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "feefineactions")
public interface FeefineactionsClient {

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  FeefineactionCollection getFeefineactions(@RequestParam String query, @RequestParam long limit);
}
