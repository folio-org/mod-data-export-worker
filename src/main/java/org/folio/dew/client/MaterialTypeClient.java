package org.folio.dew.client;

import org.folio.dew.config.feign.FeignClientConfiguration;
import org.folio.dew.domain.dto.MaterialTypeCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "material-types", configuration = FeignClientConfiguration.class)
public interface MaterialTypeClient {
  @GetMapping(value = "/{materialTypeId}", produces = MediaType.APPLICATION_JSON_VALUE) JsonNode getMaterialType(@PathVariable String materialTypeId);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  MaterialTypeCollection getByQuery(@RequestParam String query);
}
