package org.folio.dew.client;

import org.folio.dew.domain.dto.acquisitions.edifact.IdentifierType;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;


@HttpExchange(url = "identifier-types")
public interface IdentifierTypeClient {
  @GetExchange(value = "/{identifierTypeId}", accept = MediaType.APPLICATION_JSON_VALUE)
  IdentifierType getIdentifierType(@PathVariable String identifierTypeId);
}
