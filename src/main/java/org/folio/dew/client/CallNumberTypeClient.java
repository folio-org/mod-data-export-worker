package org.folio.dew.client;

import org.folio.dew.domain.dto.CallNumberType;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "call-number-types")
public interface CallNumberTypeClient {
  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  CallNumberType getById(@PathVariable String id);
}
