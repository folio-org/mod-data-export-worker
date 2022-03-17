package org.folio.dew.client;

import org.folio.dew.domain.dto.ElectronicAccessRelationship;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "electronic-access-relationships")
public interface ElectronicAccessRelationshipClient {
  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  ElectronicAccessRelationship getById(@PathVariable String id);
}
