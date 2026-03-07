package org.folio.dew.client;

import org.folio.dew.domain.dto.ServicePoint;
import org.folio.dew.domain.dto.Servicepoints;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "service-points", accept = MediaType.APPLICATION_JSON_VALUE)
public interface ServicePointClient {
  @GetExchange(value = "/{id}")
  ServicePoint getById(@PathVariable String id);

  @GetExchange
  Servicepoints get(@RequestParam String query, @RequestParam long limit);

}
