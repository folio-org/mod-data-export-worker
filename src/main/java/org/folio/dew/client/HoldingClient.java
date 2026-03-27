package org.folio.dew.client;

import org.folio.dew.domain.dto.acquisitions.edifact.Holdings;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "holdings-storage/holdings")
public interface HoldingClient {
  @GetExchange(value = "/{holdingsRecordId}", accept = MediaType.APPLICATION_JSON_VALUE)
  Holdings getHoldingById(@PathVariable String holdingsRecordId);
}
