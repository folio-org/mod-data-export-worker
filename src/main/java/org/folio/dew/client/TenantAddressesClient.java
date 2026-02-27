package org.folio.dew.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.folio.dew.config.feign.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@FeignClient(name = "tenant-addresses", configuration = FeignClientConfiguration.class)
public interface TenantAddressesClient {

  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  JsonNode getById(@PathVariable("id") String id);
}
