package org.folio.dew.service;

import static org.folio.dew.utils.Constants.EUREKA_PLATFORM;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.client.OkapiClient;
import org.folio.dew.error.NotFoundException;
import org.folio.spring.FolioExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.net.URI;

@Service
@RequiredArgsConstructor
@Log4j2
public class ModUsersModuleService {

  private static final String OKAPI_URL = "http://_";
  private static final String MOD_USERS = "mod-users";

  @Setter
  @Value("${application.platform}")
  private String platform;
  @Setter
  @Value("${application.related-modules.mod-users-id}")
  private String modUsersId;

  private final FolioExecutionContext folioExecutionContext;
  private final OkapiClient okapiClient;

  @Cacheable(cacheNames = "modUsersModuleId")
  public String getModUsersModuleId() {
    if (StringUtils.equals(EUREKA_PLATFORM, platform) && StringUtils.isNotEmpty(modUsersId)) {
      return modUsersId;
    }
    var tenantId = folioExecutionContext.getTenantId();
    var moduleNamesJson = okapiClient.getModuleIds(URI.create(OKAPI_URL), tenantId, MOD_USERS);
    if (!moduleNamesJson.isEmpty()) {
      return moduleNamesJson.get(0).get("id").asText();
    }
    var msg = "Module id not found for name: " + MOD_USERS;
    log.error(msg);
    throw new NotFoundException(msg);
  }
}
