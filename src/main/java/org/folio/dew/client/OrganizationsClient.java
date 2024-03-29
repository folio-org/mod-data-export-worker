package org.folio.dew.client;

import org.folio.dew.config.feign.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.fasterxml.jackson.databind.JsonNode;

@FeignClient(name = "organizations-storage", configuration = FeignClientConfiguration.class)
public interface OrganizationsClient {

  @GetMapping(value = "/organizations/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  JsonNode getOrganizationById(@PathVariable String id);

}
