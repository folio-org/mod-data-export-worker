package org.folio.dew.client;

import org.folio.dew.config.feign.FeignClientConfiguration;
import org.folio.dew.domain.dto.ConfigurationCollection;
import org.folio.dew.domain.dto.ModelConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "configurations/entries", configuration = FeignClientConfiguration.class)
public interface ConfigurationClient {

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  ConfigurationCollection getConfigurations(@RequestParam("query") String query);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, path = "/{entryId}")
  ModelConfiguration getConfigById(@PathVariable String entryId);
}
