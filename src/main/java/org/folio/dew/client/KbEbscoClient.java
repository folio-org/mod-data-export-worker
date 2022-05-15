package org.folio.dew.client;

import feign.Headers;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import org.folio.dew.domain.dto.eholdings.EPackage;
import org.folio.dew.domain.dto.eholdings.EResources;
import org.folio.dew.domain.dto.eholdings.EResource;

@FeignClient(name = "eholdings")
@Headers("Accept: application/vnd.api+json")
public interface KbEbscoClient {

  @GetMapping(value = "/packages/{packageId}", produces = MediaType.APPLICATION_JSON_VALUE)
  EPackage getPackageById(@PathVariable String packageId, @RequestParam String include);

  @GetMapping(value = "/resources/{resourceId}", produces = MediaType.APPLICATION_JSON_VALUE)
  EResource getResourceById(@PathVariable String resourceId, @RequestParam String include);

  @GetMapping(value = "/packages/{packageId}/resources?{filters}", produces = MediaType.APPLICATION_JSON_VALUE)
  EResources getResourcesByPackageId(@PathVariable String packageId,
                                     @PathVariable String filters,
                                     @RequestParam String include,
                                     @RequestParam int page,
                                     @RequestParam int count);
}


