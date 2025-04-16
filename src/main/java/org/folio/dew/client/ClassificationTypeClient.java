package org.folio.dew.client;

import org.folio.dew.config.feign.FeignClientConfiguration;
import org.folio.dew.domain.dto.ClassificationTypeCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "classification-types", configuration = FeignClientConfiguration.class)
public interface ClassificationTypeClient {
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  ClassificationTypeCollection getByQuery(@RequestParam String query);
}
