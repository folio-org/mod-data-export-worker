package org.folio.dew.client;

import org.folio.dew.domain.dto.HoldingsTypeCollection;
import org.folio.dew.domain.dto.TypeOfIdentifiersCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.RequestParam;


@FeignClient(name = "identifier-types")
public interface IdentifierTypeClient {
  @GetMapping(value = "/{identifierTypeId}", produces = MediaType.APPLICATION_JSON_VALUE) JsonNode getIdentifierType(@PathVariable String identifierTypeId);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  TypeOfIdentifiersCollection getByQuery(@RequestParam String query);
}
