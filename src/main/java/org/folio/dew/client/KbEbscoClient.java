package org.folio.dew.client;

import org.apache.commons.lang3.StringUtils;
import org.folio.dew.domain.dto.eholdings.EPackage;
import org.folio.dew.domain.dto.eholdings.EProvider;
import org.folio.dew.domain.dto.eholdings.EResource;
import org.folio.dew.domain.dto.eholdings.EResources;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@HttpExchange(url = "eholdings")
public interface KbEbscoClient {

  String PAGE_PARAM = "page";
  String COUNT_PARAM = "count";
  String INCLUDE_PARAM = "include";
  String ACCESS_TYPE = "accessType";
  String APPLICATION_VND_JSON_VALUE = "application/vnd.api+json";

  @GetExchange(value = "/packages/{packageId}", accept = APPLICATION_VND_JSON_VALUE)
  EPackage getPackageById(@PathVariable String packageId, @RequestParam(required = false) String include);

  @GetExchange(value = "/providers/{providerId}", accept = APPLICATION_VND_JSON_VALUE)
  EProvider getProviderById(@PathVariable String providerId, @RequestParam(required = false) String include);

  @GetExchange(value = "/resources/{resourceId}", accept = APPLICATION_VND_JSON_VALUE)
  EResource getResourceById(@PathVariable String resourceId, @RequestParam(required = false) String include);

  @GetExchange(value = "/packages/{packageId}/resources", accept = APPLICATION_VND_JSON_VALUE)
  EResources getResourcesByPackageId(@PathVariable String packageId,
                                     @RequestParam Map<String, String> parameters);

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


