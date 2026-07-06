package org.folio.dew.client;

import org.folio.dew.domain.dto.acquisitions.edifact.ContributorNameType;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;


@HttpExchange(url = "contributor-name-types")
public interface ContributorNameTypeClient {
  @GetExchange(value = "/{contributorNameTypeId}", accept = MediaType.APPLICATION_JSON_VALUE)
  ContributorNameType getContributorNameType(@PathVariable String contributorNameTypeId);
}
