package org.folio.dew.client;

import org.folio.dew.domain.dto.acquisitions.edifact.Location;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;


@HttpExchange(url = "locations", accept = MediaType.APPLICATION_JSON_VALUE)
public interface LocationClient {
  @GetExchange(value = "/{id}")
  Location getLocation(@PathVariable String id);
}
