package org.folio.dew.client;

import org.folio.dew.config.feign.FeignEncoderConfiguration;
import org.folio.dew.domain.dto.InstanceCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "inventory/instances", configuration = FeignEncoderConfiguration.class)
public interface InventoryInstancesClient {
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  InstanceCollection getInstanceByQuery(@RequestParam String query);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  InstanceCollection getInstanceByQuery(@RequestParam("query") String query, @RequestParam long limit);

}
