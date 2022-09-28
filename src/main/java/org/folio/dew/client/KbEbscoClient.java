package org.folio.dew.client;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.domain.dto.eholdings.EPackage;
import org.folio.dew.domain.dto.eholdings.EProvider;
import org.folio.dew.domain.dto.eholdings.EResource;
import org.folio.dew.domain.dto.eholdings.EResources;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "eholdings")
public interface KbEbscoClient {

  String PAGE_PARAM = "page";
  String COUNT_PARAM = "count";
  String INCLUDE_PARAM = "include";
  String ACCESS_TYPE = "accessType";
  String APPLICATION_VND_JSON_VALUE = "application/vnd.api+json";

  @GetMapping(value = "/packages/{packageId}", produces = APPLICATION_VND_JSON_VALUE)
  EPackage getPackageById(@PathVariable String packageId, @RequestParam String include);

  @GetMapping(value = "/providers/{providerId}", produces = APPLICATION_VND_JSON_VALUE)
  EProvider getProviderById(@PathVariable String providerId, @RequestParam String include);

  @GetMapping(value = "/resources/{resourceId}", produces = APPLICATION_VND_JSON_VALUE)
  EResource getResourceById(@PathVariable String resourceId, @RequestParam String include);

  @GetMapping(value = "/packages/{packageId}/resources", produces = APPLICATION_VND_JSON_VALUE)
  EResources getResourcesByPackageId(@PathVariable String packageId,
                                     @SpringQueryMap(encoded = true) Map<String, String> parameters);

  default Map<String, String> constructParams(int page, int count, String filters, String... include) {
    Map<String, String> params = new LinkedHashMap<>();
    if (StringUtils.isNotBlank(filters)) {
      Arrays.stream(filters.split("&"))
        .map(param -> param.split("="))
        .filter(ar -> ar.length == 2)
        .forEach(ar -> params.put(ar[0], ar[1]));
    }

    params.put(PAGE_PARAM, String.valueOf(page));
    params.put(COUNT_PARAM, String.valueOf(count));
    if (include.length > 0) {
      params.put(INCLUDE_PARAM, String.join(",", include));
    }
    return params;
  }
}


