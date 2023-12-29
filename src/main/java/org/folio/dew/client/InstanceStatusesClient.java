package org.folio.dew.client;

import org.folio.dew.config.feign.FeignClientConfiguration;
import org.folio.dew.domain.dto.InstanceStatus;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "instance-statuses", configuration = FeignClientConfiguration.class)
public interface InstanceStatusesClient {
  @GetMapping(value = "/{instanceStatusId}", produces = MediaType.APPLICATION_JSON_VALUE)
  InstanceStatus getById(@PathVariable String instanceStatusId);
}
