package org.folio.dew.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.folio.dew.config.feign.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "source-storage", configuration = FeignClientConfiguration.class)
public interface SrsClient {

  @GetMapping(value = "/source-records", produces = MediaType.APPLICATION_JSON_VALUE)
  JsonNode getMarc(@RequestParam("instanceId") String instanceId, @RequestParam("idType") String idType, @RequestParam("deleted") boolean deleted);
}
