package org.folio.dew.client;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "user-tenants", accept = MediaType.APPLICATION_JSON_VALUE)
public interface UserTenantsClient {

  @GetExchange
  UserTenants getUserTenants(@RequestParam("tenantId") String tenantId);

  record UserTenants(List<UserTenant> userTenants) {
  }

  record UserTenant(String centralTenantId, String consortiumId) {
  }
}
