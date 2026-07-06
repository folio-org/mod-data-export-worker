package org.folio.dew.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

/**
 * Client for {@code mgr-tenant-entitlements}. Used to discover which application/modules a tenant is
 * entitled to — in particular the currently deployed {@code mod-orders-storage} module id, which is
 * needed as the {@code X-Okapi-Module-Id} when calling the {@code interfaceType: multiple}
 * {@code custom-fields} interface.
 */
@HttpExchange(url = "entitlements", accept = MediaType.APPLICATION_JSON_VALUE)
public interface EntitlementsClient {

  @GetExchange
  EntitlementCollection getEntitlements(@RequestParam("includeModules") boolean includeModules,
                                        @RequestParam("limit") int limit);

  @JsonIgnoreProperties(ignoreUnknown = true)
  record EntitlementCollection(List<Entitlement> entitlements) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Entitlement(String tenantId, String applicationId, List<String> modules) {
  }
}