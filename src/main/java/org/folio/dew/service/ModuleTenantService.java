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
import java.util.Optional;


@Log4j2
@Service
@RequiredArgsConstructor
public class ModuleTenantService {

  private static final String MODULE_NOT_FOUND_ERROR = "Module id not found for name: ";
  private static final String URL_PREFIX = "http://_";

  @Setter
  @Value("${application.platform}")
  private String platform;

  private final FolioExecutionContext folioExecutionContext;
  private final OkapiClient okapiClient;
  private final EurekaProxyTenantsClient eurekaProxyTenantsClient;

  @Cacheable(cacheNames = "moduleIds")
  public String getModuleId(String moduleName) {
    Optional<String> moduleId;
    if (StringUtils.equals(EUREKA_PLATFORM, platform)) {
      moduleId = getModuleIdForEureka(moduleName);
    } else {
      moduleId = getModuleIdForOkapi(moduleName);
    }
    var msg = MODULE_NOT_FOUND_ERROR + moduleName;
    return moduleId.orElseThrow(() -> new NotFoundException(msg));
  }

  private Optional<String> getModuleIdForOkapi(String moduleName) {
    var tenantId = folioExecutionContext.getTenantId();
    var modules = okapiClient.getModuleIds(URI.create(URL_PREFIX), tenantId, moduleName);
    if (!modules.isEmpty()) {
      return Optional.of(modules.get(0).getId());
    }
    return Optional.empty();
  }

  private Optional<String> getModuleIdForEureka(String moduleName) {
    log.info("getModuleIdForEureka");
    var modules = eurekaProxyTenantsClient.getModules(URI.create(URL_PREFIX), folioExecutionContext.getTenantId());
    log.info(modules);
    return modules.stream().filter(module -> StringUtils.equals(moduleName, module.getName())).findFirst().map(ModuleForTenant::getId);
  }
}
