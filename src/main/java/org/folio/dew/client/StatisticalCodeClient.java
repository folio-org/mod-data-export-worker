package org.folio.dew.client;

import org.folio.dew.domain.dto.StatisticalCode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "statistical-codes")
public interface StatisticalCodeClient {
  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  StatisticalCode getById(@PathVariable String id);
}
