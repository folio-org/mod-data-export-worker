package org.folio.dew.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.folio.dew.domain.dto.eholdings.EPackage;
import org.folio.dew.domain.dto.eholdings.EResources;
import org.folio.dew.domain.dto.eholdings.EResource;
import org.folio.dew.domain.dto.eholdings.ETitle;

@FeignClient(name = "eholdings")
public interface KbEbscoClient {

  @GetMapping(value = "/packages/{packageId}", produces = MediaType.APPLICATION_JSON_VALUE)
  EPackage getPackageById(@PathVariable String packageId);

  @GetMapping(value = "/titles/{titleId}", produces = MediaType.APPLICATION_JSON_VALUE)
  ETitle getTitleById(@PathVariable String titleId);

  @GetMapping(value = "resources/{resourceId}", produces = MediaType.APPLICATION_JSON_VALUE)
  EResource getResourceById(@PathVariable String resourceId);

  @GetMapping(value = "/packages/{packageId}/resources?{query}",produces = MediaType.APPLICATION_JSON_VALUE)
  EResources getResourcesByPackageId(@PathVariable String packageId, @PathVariable String query);
}


