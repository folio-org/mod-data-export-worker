package org.folio.dew.batch.acquisitions.edifact.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.PathVariable;


@FeignClient(name = "material-types")
public interface MaterialTypeClient {
  @GetMapping(value = "/{materialTypeId}", produces = MediaType.APPLICATION_JSON_VALUE)
  JSONObject getMaterialType(@PathVariable String materialTypeId);

}
