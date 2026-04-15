package org.folio.dew.client;

import org.folio.dew.domain.dto.authority.control.AuthorityDataStatDto;
import org.folio.dew.domain.dto.authority.control.AuthorityDataStatDtoCollection;
import org.folio.dew.domain.dto.authority.control.InstanceDataStatDtoCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "links")
public interface EntitiesLinksStatsClient {

  @GetExchange(value = "/stats/authority", accept = MediaType.APPLICATION_JSON_VALUE)
  AuthorityDataStatDtoCollection getAuthorityStats(@RequestParam int limit,
                                                   @RequestParam AuthorityDataStatDto.ActionEnum action,
                                                   @RequestParam String fromDate,
                                                   @RequestParam String toDate);

  @GetExchange(value = "/stats/instance", accept = MediaType.APPLICATION_JSON_VALUE)
  InstanceDataStatDtoCollection getInstanceStats(@RequestParam int limit,
                                                 @RequestParam LinkStatus status,
                                                 @RequestParam(required = false) String fromDate,
                                                 @RequestParam(required = false) String toDate);
  enum LinkStatus {ERROR, ACTION}
}


