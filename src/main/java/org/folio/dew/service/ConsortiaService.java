package org.folio.dew.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.client.ConsortiaClient;
import org.folio.spring.FolioExecutionContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class ConsortiaService {

  private final ConsortiaClient consortiaClient;
  private final FolioExecutionContext context;

  @Cacheable(value = "centralTenantCache")
  public String getCentralTenantId(String currentTenantId) {
    var userTenantCollection = consortiaClient.getUserTenantCollection();
    var userTenants = userTenantCollection.getUserTenants();
    if (!userTenants.isEmpty()) {
      return userTenants.get(0).getCentralTenantId();
    }
    return StringUtils.EMPTY;
  }

  @Cacheable(value = "isCurrentTenantCentralTenant")
  public boolean isCurrentTenantCentralTenant(String currentTenantId) {
    return getCentralTenantId(currentTenantId).equals(context.getTenantId());
  }
}
