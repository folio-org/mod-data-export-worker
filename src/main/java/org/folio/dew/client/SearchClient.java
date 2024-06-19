package org.folio.dew.client;

import org.folio.dew.domain.dto.BatchIdsDto;
import org.folio.dew.domain.dto.ConsortiumHoldingCollection;
import org.folio.dew.domain.dto.ConsortiumItemCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "search")
public interface SearchClient {
  @GetMapping(value = "/instances/ids", headers = {"Accept=text/plain"})
  ResponseEntity<InputStreamResource> getInstanceIds(@RequestParam String query);

  @GetMapping(value = "/holdings/ids", headers = {"Accept=text/plain"})
  ResponseEntity<InputStreamResource> getHoldingIds(@RequestParam String query);

  @PostMapping(value = "/consortium/batch/items", headers = {"Accept=application/json"})
  ConsortiumItemCollection getConsortiumItemCollection(@RequestBody BatchIdsDto batchIdsDto);

  @PostMapping(value = "/consortium/batch/holdings", headers = {"Accept=application/json"})
  ConsortiumHoldingCollection getConsortiumHoldingCollection(@RequestBody BatchIdsDto batchIdsDto);
}
