package org.folio.dew.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

/**
 * Client for {@code mgr-tenants}. Resolves a tenant name to its id (UUID), used to scope the
 * cross-tenant {@code /entitlements} response to the current tenant when more than one tenant exists.
 */
@HttpExchange(url = "tenants", accept = MediaType.APPLICATION_JSON_VALUE)
public interface TenantsClient {

  @GetExchange
  TenantCollection getTenants(@RequestParam("query") String query, @RequestParam("limit") int limit);

  @JsonIgnoreProperties(ignoreUnknown = true)
  record TenantCollection(List<Tenant> tenants) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Tenant(String id, String name) {
  }
}