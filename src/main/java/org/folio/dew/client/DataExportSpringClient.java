package org.folio.dew.client;

import org.folio.dew.domain.dto.Job;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "data-export-spring")
public interface DataExportSpringClient {

  @GetMapping(value = "/jobs/{jobId}", produces = MediaType.APPLICATION_JSON_VALUE)
  Job getJobById(@PathVariable String jobId);

}
