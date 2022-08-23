package org.folio.dew.client;

import org.folio.dew.config.feign.FeignClientConfiguration;
import org.folio.dew.domain.dto.BriefHoldingsRecordCollection;
import org.folio.dew.domain.dto.HoldingsRecordCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "holdings-storage/holdings", configuration = FeignClientConfiguration.class)
public interface HoldingClient {
  @GetMapping(value = "/{holdingsRecordId}", produces = MediaType.APPLICATION_JSON_VALUE)
  JsonNode getHoldingById(@PathVariable String holdingsRecordId);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  BriefHoldingsRecordCollection getBriefHoldingsByQuery(@RequestParam String query);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  HoldingsRecordCollection getHoldingsByQuery(@RequestParam String query);
}
