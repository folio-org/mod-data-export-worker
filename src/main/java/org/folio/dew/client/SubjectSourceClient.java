package org.folio.dew.client;

import org.folio.dew.config.feign.FeignClientConfiguration;
import org.folio.dew.domain.dto.SubjectSourceCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "subject-sources", configuration = FeignClientConfiguration.class)
public interface SubjectSourceClient {
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  SubjectSourceCollection getByQuery(@RequestParam String query);
}
