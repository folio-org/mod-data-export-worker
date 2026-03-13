package org.folio.dew.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.folio.dew.config.feign.FeignClientConfiguration;
import org.folio.dew.domain.dto.templateengine.TemplateProcessingRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "template-request", configuration = FeignClientConfiguration.class)
public interface TemplateEngineClient {

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  JsonNode processTemplate(@RequestBody TemplateProcessingRequest request);

}
