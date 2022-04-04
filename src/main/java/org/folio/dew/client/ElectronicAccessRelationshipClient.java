package org.folio.dew.client;

import org.folio.dew.config.feign.FeignClientConfiguration;
import org.folio.dew.domain.dto.ElectronicAccessRelationship;
import org.folio.dew.domain.dto.ElectronicAccessRelationshipCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "electronic-access-relationships", configuration = FeignClientConfiguration.class)
public interface ElectronicAccessRelationshipClient {
  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  ElectronicAccessRelationship getById(@PathVariable String id);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  ElectronicAccessRelationshipCollection getByQuery(@RequestParam String query);
}
