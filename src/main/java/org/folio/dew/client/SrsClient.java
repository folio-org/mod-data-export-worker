package org.folio.dew.client;

import org.folio.dew.config.feign.FeignClientConfiguration;
import org.folio.dew.domain.dto.SrsRecordCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "source-storage", configuration = FeignClientConfiguration.class)
public interface SrsClient {

  @GetMapping(value = "/source-records")
  SrsRecordCollection getMarc(@RequestParam("instanceId") String instanceId, @RequestParam("idType") String idType);
}
