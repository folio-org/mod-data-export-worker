package org.folio.dew.client;

import org.folio.dew.config.feign.FeignClientConfiguration;
import org.folio.dew.domain.dto.InstanceType;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
@FeignClient(name = "instance-types", configuration = FeignClientConfiguration.class)
public interface InstanceTypesClient {
  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  InstanceType getById(@PathVariable String id);
}
