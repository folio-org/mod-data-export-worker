package org.folio.dew.client;

import org.folio.dew.domain.dto.ExportConfigCollection;
import org.folio.dew.domain.dto.Job;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "data-export-spring")
public interface DataExportSpringClient {

  @GetMapping(value = "/jobs/{jobId}", produces = MediaType.APPLICATION_JSON_VALUE)
  Job getJobById(@PathVariable String jobId);

  @GetMapping(value = "/configs", produces = MediaType.APPLICATION_JSON_VALUE)
  ExportConfigCollection getExportConfigs(@RequestParam("query") String query);

}
