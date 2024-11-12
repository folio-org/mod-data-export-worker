package org.folio.dew.service;

import static org.folio.dew.utils.Constants.EUREKA_PLATFORM;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.client.EurekaProxyTenantsClient;
import org.folio.dew.client.OkapiClient;
import org.folio.dew.domain.bean.ModuleForTenant;
import org.folio.dew.error.NotFoundException;
import org.folio.spring.FolioExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.Optional;


@Log4j2
@Service
@RequiredArgsConstructor
public class ModuleTenantService {

  private static final String MODULE_NOT_FOUND_ERROR = "Module id not found for name: ";
  private static final String URL_PREFIX = "http://_";
  private static final String MOD_USERS = "mod-users";

  @Setter
  @Value("${application.platform}")
  private String platform;

  private final FolioExecutionContext folioExecutionContext;
  private final OkapiClient okapiClient;
  private final EurekaProxyTenantsClient eurekaProxyTenantsClient;

  @Cacheable(cacheNames = "moduleIds")
  public String getModUsersModuleId() {
    Optional<String> moduleId;
    if (StringUtils.equals(EUREKA_PLATFORM, platform)) {
      moduleId = getModUsersModuleIdForEureka();
    } else {
      moduleId = getModUsersModuleIdForOkapi();
    }
    var msg = MODULE_NOT_FOUND_ERROR + MOD_USERS;
    return moduleId.orElseThrow(() -> new NotFoundException(msg));
  }

  private Optional<String> getModUsersModuleIdForOkapi() {
    var tenantId = folioExecutionContext.getTenantId();
    var modules = okapiClient.getModuleIds(URI.create(URL_PREFIX), tenantId, MOD_USERS);
    if (!modules.isEmpty()) {
      return Optional.of(modules.get(0).getId());
    }
    return Optional.empty();
  }

  private Optional<String> getModUsersModuleIdForEureka() {
    var modules = eurekaProxyTenantsClient.getModules(URI.create(URL_PREFIX), folioExecutionContext.getTenantId());
    return filterModUsersModuleId(modules);
  }

  private Optional<String> filterModUsersModuleId(List<ModuleForTenant> modules) {
    return modules.stream().map(ModuleForTenant::getId).filter(id -> id.matches("^mod-users-\\d.*?$")).findFirst();
  }
}
