package org.folio.dew.service;

import static org.folio.dew.utils.Constants.EUREKA_PLATFORM;

import java.util.regex.Pattern;
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

  private static final String URL_PREFIX = "http://_";
  private static final String MOD_USERS = "mod-users";
  private static final String MOD_USERS_NOT_FOUND_ERROR = "Module id not found for name: " + MOD_USERS;
  private static final String MOD_USERS_REGEXP = "^mod-users-\\d.*$";
  private static final Pattern MOD_USERS_PATTERN = Pattern.compile(MOD_USERS_REGEXP);

  @Setter
  @Value("${application.platform}")
  private String platform;

  private final FolioExecutionContext folioExecutionContext;
  private final OkapiClient okapiClient;
  private final EurekaProxyTenantsClient eurekaProxyTenantsClient;

  @Cacheable(cacheNames = "modUsersModuleIds")
  public String getModUsersModuleId() {
    var moduleId = StringUtils.equals(EUREKA_PLATFORM, platform) ? getModUsersModuleIdForEureka() : getModUsersModuleIdForOkapi();
    return moduleId.orElseThrow(() -> new NotFoundException(MOD_USERS_NOT_FOUND_ERROR));
  }

  private Optional<String> getModUsersModuleIdForOkapi() {
    var tenantId = folioExecutionContext.getTenantId();
    var modules = okapiClient.getModuleIds(URI.create(URL_PREFIX), tenantId, MOD_USERS);
    if (!modules.isEmpty()) {
      return Optional.of(modules.getFirst().getId());
    }
    return Optional.empty();
  }

  private Optional<String> getModUsersModuleIdForEureka() {
    var modules = eurekaProxyTenantsClient.getModules(URI.create(URL_PREFIX), folioExecutionContext.getTenantId());
    return filterModUsersModuleId(modules);
  }

  private Optional<String> filterModUsersModuleId(List<ModuleForTenant> modules) {
    return modules.stream()
        .map(ModuleForTenant::getId)
        .filter(id -> MOD_USERS_PATTERN.matcher(id).matches())
        .findFirst();
  }
}
