package org.folio.dew.client;

import org.folio.dew.config.feign.FeignClientConfiguration;
import org.folio.dew.domain.bean.ModuleForTenant;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URI;
import java.util.List;

@FeignClient(name = "okapi", configuration = FeignClientConfiguration.class)
public interface OkapiClient {
  @GetMapping(value = "/proxy/tenants/{tenantId}/modules", produces = MediaType.APPLICATION_JSON_VALUE)
  List<ModuleForTenant> getModuleIds(URI uri, @PathVariable("tenantId") String tenantId, @RequestParam("filter") String moduleName);
}
