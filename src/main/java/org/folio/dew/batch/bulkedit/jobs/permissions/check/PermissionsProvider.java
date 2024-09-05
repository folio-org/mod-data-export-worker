package org.folio.dew.batch.bulkedit.jobs.permissions.check;

import lombok.RequiredArgsConstructor;
import org.folio.dew.client.PermissionsSelfCheckClient;
import org.folio.dew.service.FolioExecutionContextManager;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PermissionsProvider extends FolioExecutionContextManager {

  private final PermissionsSelfCheckClient permissionsSelfCheckClient;
  private final FolioExecutionContext folioExecutionContext;

  @Cacheable(cacheNames = "desiredPermissions")
  public List<String> getDesiredPermissions(String tenantId) {
    try (var ignored = new FolioExecutionContextSetter(refreshAndGetFolioExecutionContext(tenantId, folioExecutionContext))) {
      return permissionsSelfCheckClient.getDesiredPermissions();
    }
  }
}
