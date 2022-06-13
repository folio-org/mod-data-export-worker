package org.folio.dew.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "search")
public interface SearchClient {
  @GetMapping(value = "/instances/ids", headers = {"Accept=text/plain"})
  ResponseEntity<InputStreamResource> getInstanceIds(@RequestParam String query);

  @GetMapping(value = "/holdings/ids", headers = {"Accept=text/plain"})
  ResponseEntity<InputStreamResource> getHoldingIds(@RequestParam String query);
}
