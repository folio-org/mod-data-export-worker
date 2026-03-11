package org.folio.dew.client;

import org.folio.dew.domain.dto.acquisitions.edifact.Organization;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "organizations-storage", accept = MediaType.APPLICATION_JSON_VALUE)
public interface OrganizationsClient {

  @GetExchange(value = "/organizations/{id}")
  Organization getOrganizationById(@PathVariable String id);

}
