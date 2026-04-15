package org.folio.dew.client;

import org.folio.dew.domain.dto.acquisitions.edifact.TenantAddress;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;


@HttpExchange(url = "tenant-addresses", accept = MediaType.APPLICATION_JSON_VALUE)
public interface TenantAddressesClient {

  @GetExchange(value = "/{id}")
  TenantAddress getById(@PathVariable("id") String id);
}
