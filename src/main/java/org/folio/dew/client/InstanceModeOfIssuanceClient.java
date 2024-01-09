package org.folio.dew.client;

import org.folio.dew.config.feign.FeignClientConfiguration;
import org.folio.dew.domain.dto.IssuanceMode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "modes-of-issuance", configuration = FeignClientConfiguration.class)
public interface InstanceModeOfIssuanceClient {
  @GetMapping(value = "/{modeOfIssuanceId}", produces = MediaType.APPLICATION_JSON_VALUE)
  IssuanceMode getById(@PathVariable String modeOfIssuanceId);
}
