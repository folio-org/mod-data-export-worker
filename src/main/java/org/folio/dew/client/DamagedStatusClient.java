package org.folio.dew.client;

import org.folio.dew.domain.dto.DamagedStatus;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "item-damaged-statuses")
public interface DamagedStatusClient {
  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  DamagedStatus getById(@PathVariable String id);
}
