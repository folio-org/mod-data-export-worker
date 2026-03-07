package org.folio.dew.client;

import org.folio.dew.domain.dto.MaterialType;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "material-types")
public interface MaterialTypeClient {
  @GetExchange  (value = "/{materialTypeId}", accept = MediaType.APPLICATION_JSON_VALUE)
  MaterialType getMaterialType(@PathVariable String materialTypeId);
}
