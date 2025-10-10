package org.folio.dew.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.client.UserTenantsClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class UserTenantsService {

  private final UserTenantsClient userTenantsClient;

  @Cacheable(cacheNames = "consortium-central-tenant-cache", key = "@folioExecutionContext.tenantId + ':' + #tenantId")
  public Optional<String> getCentralTenant(String tenantId) {
    if (StringUtils.isBlank(tenantId)) {
      return Optional.empty();
    }

    var userTenants = userTenantsClient.getUserTenants(tenantId);
    log.debug("getCentralTenant:  tenantId: {}, response: {}", tenantId, userTenants);

    return Optional.ofNullable(userTenants)
      .map(UserTenantsClient.UserTenants::userTenants)
      .orElse(List.of())
      .stream()
      .findFirst()
      .map(UserTenantsClient.UserTenant::centralTenantId);
  }
}
