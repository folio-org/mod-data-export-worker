package org.folio.dew.batch.bulkedit.jobs.permissions.check;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.service.FolioExecutionContextManager;
import org.folio.dew.service.UserPermissionsService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Log4j2
public class PermissionsProvider extends FolioExecutionContextManager {

  private final FolioExecutionContext folioExecutionContext;
  private final UserPermissionsService userPermissionsService;

  @Cacheable(cacheNames = "userPermissions")
  public List<String> getUserPermissions(String tenantId, String userId) {
    try (var ignored = new FolioExecutionContextSetter(refreshAndGetFolioExecutionContext(tenantId, folioExecutionContext))) {
      log.info("getUserPermissions:: user {} tenant {}", userId, tenantId);
      return userPermissionsService.getPermissions();
    }
  }
}
