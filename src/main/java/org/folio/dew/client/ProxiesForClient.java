package org.folio.dew.client;

import org.folio.dew.config.feign.FeignClientConfiguration;
import org.folio.dew.domain.dto.ProxyFor;
import org.folio.dew.domain.dto.ProxyForCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "proxiesfor", configuration = FeignClientConfiguration.class)
public interface ProxiesForClient {

  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  ProxyFor getProxiesForById(@PathVariable String id);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  ProxyForCollection getProxiesForByQuery(@RequestParam String query);
}
