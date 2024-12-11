package org.folio.dew.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.client.ElectronicAccessRelationshipClient;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class ElectronicAccessServiceCache extends FolioExecutionContextManager {

  private final ElectronicAccessRelationshipClient relationshipClient;
  private final FolioExecutionContext folioExecutionContext;

  @Cacheable(cacheNames = "relationships")
  public String getRelationshipNameById(String id, String tenantId) {
    try (var context = new FolioExecutionContextSetter(refreshAndGetFolioExecutionContext(tenantId, folioExecutionContext))) {
      return relationshipClient.getById(id).getName();
    }
  }
}
