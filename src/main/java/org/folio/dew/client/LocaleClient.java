package org.folio.dew.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.folio.dew.config.feign.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;


@FeignClient(name = "locale", configuration = FeignClientConfiguration.class)
public interface LocaleClient {

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  JsonNode getLocale();
}
