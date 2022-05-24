package org.folio.dew.client;

import java.util.HashMap;
import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import org.folio.dew.domain.dto.eholdings.EPackage;
import org.folio.dew.domain.dto.eholdings.EResource;
import org.folio.dew.domain.dto.eholdings.EResources;

@FeignClient(name = "eholdings")
public interface KbEbscoClient {

  String PAGE_PARAM = "page";
  String COUNT_PARAM = "count";
  String INCLUDE_PARAM = "include";
  String ACCESS_TYPE = "accessType";

  @GetMapping(value = "/packages/{packageId}", produces = "application/vnd.api+json")
  EPackage getPackageById(@PathVariable String packageId, @RequestParam String include);

  @GetMapping(value = "/resources/{resourceId}", produces = "application/vnd.api+json")
  EResource getResourceById(@PathVariable String resourceId, @RequestParam String include);

  @GetMapping(value = "/packages/{packageId}/resources", produces = "application/vnd.api+json")
  EResources getResourcesByPackageId(@PathVariable String packageId, @SpringQueryMap(encoded = true) Map<String, String> parameters);

  default Map<String, String> constructParams(int page, int count, String filters, String... include) {
    var params = new HashMap<String, String>();
    params.put(filters, null);
    params.put(PAGE_PARAM, String.valueOf(page));
    params.put(COUNT_PARAM, String.valueOf(count));
    if (include.length > 0) {
      params.put(INCLUDE_PARAM, String.join(",", include));
    }
    return params;
  }
}


