package org.folio.dew.client;

import org.folio.dew.domain.dto.authority.control.AuthorityDataStatDto;
import org.folio.dew.domain.dto.authority.control.AuthorityDataStatDtoCollection;
import org.folio.dew.domain.dto.authority.control.InstanceDataLinkDtoCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "links")
public interface EntitiesLinksStatsClient {

  @GetMapping(value = "/authority/stats", produces = MediaType.APPLICATION_JSON_VALUE)
  AuthorityDataStatDtoCollection getAuthorityStats(@RequestParam int limit,
                                                   @RequestParam AuthorityDataStatDto.ActionEnum action,
                                                   @RequestParam String fromDate,
                                                   @RequestParam String toDate);

  @GetMapping(value = "/instance/stats", produces = MediaType.APPLICATION_JSON_VALUE)
  InstanceDataLinkDtoCollection getInstanceStats(@RequestParam int limit,
                                                 @RequestParam String fromDate,
                                                 @RequestParam String toDate);
}


