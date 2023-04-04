package org.folio.dew.client;

import org.folio.dew.domain.dto.authority.control.AuthorityDataStatDto;
import org.folio.dew.domain.dto.authority.control.AuthorityDataStatDtoCollection;
import org.folio.dew.domain.dto.authority.control.InstanceDataStatDtoCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "links")
public interface EntitiesLinksStatsClient {

  @GetMapping(value = "/stats/authority", produces = MediaType.APPLICATION_JSON_VALUE)
  AuthorityDataStatDtoCollection getAuthorityStats(@RequestParam int limit,
                                                   @RequestParam AuthorityDataStatDto.ActionEnum action,
                                                   @RequestParam String fromDate,
                                                   @RequestParam String toDate);

  @GetMapping(value = "/stats/instance", produces = MediaType.APPLICATION_JSON_VALUE)
  InstanceDataStatDtoCollection getInstanceStats(@RequestParam int limit,
                                                 @RequestParam LinkStatus status,
                                                 @RequestParam String fromDate,
                                                 @RequestParam String toDate);
  enum LinkStatus {ERROR, ACTION}
}


