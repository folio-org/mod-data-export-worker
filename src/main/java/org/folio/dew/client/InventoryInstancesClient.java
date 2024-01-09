package org.folio.dew.client;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.folio.dew.config.feign.FeignEncoderConfiguration;
import org.folio.dew.domain.dto.Instance;
import org.folio.dew.domain.dto.InstanceCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "inventory/instances", configuration = FeignEncoderConfiguration.class)
public interface InventoryInstancesClient {
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  InstanceCollection getInstanceByQuery(@RequestParam String query);

  @GetMapping(value = "/{instanceId}", produces = MediaType.APPLICATION_JSON_VALUE)
  Instance getInstanceById(@PathVariable String instanceId);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  InstanceCollection getInstanceByQuery(@RequestParam("query") String query, @RequestParam long limit);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  InstanceCollection getInstanceByQuery(@RequestParam("query") String query, @RequestParam long offset, @RequestParam long limit);

  @PutMapping(value = "/{instanceId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  void updateInstance(@RequestBody Instance instance, @PathVariable String instanceId);
}
