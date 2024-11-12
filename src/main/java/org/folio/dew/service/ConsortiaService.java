package org.folio.dew.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.client.ConsortiaClient;
import org.folio.dew.client.ConsortiumClient;
import org.folio.dew.domain.dto.UserTenant;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class ConsortiaService {

  private final ConsortiaClient consortiaClient;
  private final ConsortiumClient consortiumClient;

  @Cacheable(value = "centralTenantCache")
  public String getCentralTenantId() {
    var userTenantCollection = consortiaClient.getUserTenantCollection();
    var userTenants = userTenantCollection.getUserTenants();
    if (!userTenants.isEmpty()) {
      return userTenants.get(0).getCentralTenantId();
    }
    return StringUtils.EMPTY;
  }

  @Cacheable(value = "affiliatedTenantsCache")
  public List<String> getAffiliatedTenants(String currentTenantId, String userId) {
    var consortia = consortiumClient.getConsortia();
    var consortiaList = consortia.getConsortia();
    if (!consortiaList.isEmpty()) {
      var userTenants = consortiumClient.getConsortiaUserTenants(consortiaList.get(0).getId(), userId, Integer.MAX_VALUE);
      return userTenants.getUserTenants().stream().map(UserTenant::getTenantId).toList();
    }
    return new ArrayList<>();
  }
}
