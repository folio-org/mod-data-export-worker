package org.folio.dew.batch.acquisitions.edifact.client;

import org.json.JSONObject;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@FeignClient(name = "holdings-storage")
public interface HoldingClient
{
  @GetMapping(value = "/holdings/{holdingId}", produces = MediaType.APPLICATION_JSON_VALUE)
  JSONObject getHolding(@PathVariable String holdingId);

}
