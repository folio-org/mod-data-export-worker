package org.folio.dew.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.folio.dew.domain.dto.BriefInstance;
import org.folio.dew.domain.dto.BriefInstanceCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "instance-storage/instances")
public interface InstanceClient {
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  BriefInstanceCollection getByQuery(@RequestParam String query);

  @GetMapping(value = "/{instanceId}", produces = MediaType.APPLICATION_JSON_VALUE)
  BriefInstance getById(@PathVariable String instanceId);

  @GetMapping(value = "/{instanceId}", produces = MediaType.APPLICATION_JSON_VALUE)
  JsonNode getInstanceJsonById(@PathVariable String instanceId);
}
