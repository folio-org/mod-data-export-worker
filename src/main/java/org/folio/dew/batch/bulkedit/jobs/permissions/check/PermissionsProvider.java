package org.folio.dew.batch.bulkedit.jobs.permissions.check;

import lombok.RequiredArgsConstructor;
import org.folio.dew.client.PermissionsSelfCheckClient;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.folio.dew.utils.FolioExecutionContextUtil.prepareContextForTenant;

@Component
@RequiredArgsConstructor
public class PermissionsProvider {

  private final PermissionsSelfCheckClient permissionsSelfCheckClient;
  private final FolioExecutionContext folioExecutionContext;
  private final FolioModuleMetadata folioModuleMetadata;

  @Cacheable(cacheNames = "desiredPermissions")
  public List<String> getDesiredPermissions(String tenantId) {
    try (var ignored = new FolioExecutionContextSetter(prepareContextForTenant(tenantId, folioModuleMetadata, folioExecutionContext))) {
      return permissionsSelfCheckClient.getDesiredPermissions();
    }
  }
}
