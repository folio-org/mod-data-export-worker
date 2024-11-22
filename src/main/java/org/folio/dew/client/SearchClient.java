package org.folio.dew.client;

import org.folio.dew.domain.dto.BatchIdsDto;
import org.folio.dew.domain.dto.ConsortiumHoldingCollection;
import org.folio.dew.domain.dto.ConsortiumItemCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "search")
public interface SearchClient {
  @PostMapping(value = "/consortium/batch/items", headers = {"Accept=application/json"})
  ConsortiumItemCollection getConsortiumItemCollection(@RequestBody BatchIdsDto batchIdsDto);

  @PostMapping(value = "/consortium/batch/holdings", headers = {"Accept=application/json"})
  ConsortiumHoldingCollection getConsortiumHoldingCollection(@RequestBody BatchIdsDto batchIdsDto);
}
