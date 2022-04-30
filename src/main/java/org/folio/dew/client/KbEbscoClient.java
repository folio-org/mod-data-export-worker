package org.folio.dew.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "eholdings")
public interface KbEbscoClient {

  @GetMapping(value = "/packages/{packageId}", produces = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<String> getPackageById(@PathVariable String packageId);

  @GetMapping(value = "/titles/{titleId}", produces = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<String> getTitleById(@PathVariable String titleId);

  @GetMapping(value = "/packages/{packageId}/resources?{query}",produces = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<String> getResourcesByPackageId(@PathVariable String packageId, @PathVariable String query);
}


