package org.folio.dew.client;

import org.folio.dew.domain.dto.templateengine.TemplateProcessingRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(url = "template-request", contentType = MediaType.APPLICATION_JSON_VALUE)
public interface TemplateEngineClient {

  @PostExchange
  String processTemplate(@RequestBody TemplateProcessingRequest request);

}
