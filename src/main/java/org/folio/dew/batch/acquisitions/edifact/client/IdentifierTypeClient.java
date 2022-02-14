package org.folio.dew.batch.acquisitions.edifact.client;

import org.json.JSONObject;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@FeignClient(name = "identifier-types")
public interface IdentifierTypeClient
{
  @GetMapping(value = "/{identifierTypeId}", produces = MediaType.APPLICATION_JSON_VALUE)
  JSONObject getIdentifierType(@PathVariable String identifierTypeId);

}
