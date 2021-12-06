package org.folio.dew.client;

import org.folio.dew.domain.dto.ProxyFor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "proxiesfor")
public interface ProxiesForClient {

  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  ProxyFor getProxiesForById(@PathVariable String id);
}
