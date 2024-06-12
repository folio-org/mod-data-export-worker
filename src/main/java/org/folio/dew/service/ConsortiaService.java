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

  private final FolioExecutionContext context;

  private final ConsortiaClient consortiaClient;

  @Cacheable(value = "centralTenantCache")
  public String getCentralTenantId() {
    var userTenantCollection = consortiaClient.getUserTenantCollection();
    var userTenants = userTenantCollection.getUserTenants();
    if (!userTenants.isEmpty()) {
      log.info("userTenants: {}", userTenants);
      var centralTenantId = userTenants.get(0).getCentralTenantId();
      if (centralTenantId.equals(context.getTenantId())) {
        log.error("Current tenant is central");
      }
      return centralTenantId;
    }
    log.info("No central tenant found");
    return StringUtils.EMPTY;
  }
}
