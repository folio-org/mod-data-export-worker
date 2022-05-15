package org.folio.dew.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import org.folio.dew.domain.dto.eholdings.EPackage;
import org.folio.dew.domain.dto.eholdings.EResources;
import org.folio.dew.domain.dto.eholdings.EResource;

@FeignClient(name = "eholdings")
public interface KbEbscoClient {

  @GetMapping(value = "/packages/{packageId}", produces = "application/vnd.api+json")
  EPackage getPackageById(@PathVariable String packageId, @RequestParam String include);

  @GetMapping(value = "/resources/{resourceId}", produces = "application/vnd.api+json")
  EResource getResourceById(@PathVariable String resourceId, @RequestParam String include);

  @GetMapping(value = "/packages/{packageId}/resources?{filters}", produces = "application/vnd.api+json")
  EResources getResourcesByPackageId(@PathVariable String packageId,
                                     @PathVariable String filters,
                                     @RequestParam String include,
                                     @RequestParam int page,
                                     @RequestParam int count);
}


