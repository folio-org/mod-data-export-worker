package org.folio.dew.client;

import org.folio.dew.config.feign.FeignClientConfiguration;
import org.folio.dew.domain.dto.IllPolicy;
import org.folio.dew.domain.dto.IllPolicyCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ill-policies", configuration = FeignClientConfiguration.class)
public interface IllPolicyClient {
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  IllPolicyCollection getByQuery(@RequestParam String query);

  @GetMapping(value = "/{illPolicyId}")
  IllPolicy getById(@PathVariable String illPolicyId);
}
