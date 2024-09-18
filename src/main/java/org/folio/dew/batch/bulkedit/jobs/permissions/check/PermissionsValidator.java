package org.folio.dew.batch.bulkedit.jobs.permissions.check;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.EntityType;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
public class PermissionsValidator {

  private static final String BULK_EDIT_ITEM_POST_PERMISSION = "bulk-edit.item.post";
  private final PermissionsProvider permissionsProvider;
  private final RequiredPermissionResolver requiredPermissionResolver;
  private final FolioExecutionContext folioExecutionContext;


  public boolean isBulkEditReadPermissionExists(String tenantId, EntityType entityType) {
    var readPermissionForEntity = requiredPermissionResolver.getReadPermission(entityType);
    var userPermissions = permissionsProvider.getUserPermissions(tenantId);
    var isReadPermissionsExist = userPermissions.contains(readPermissionForEntity) && userPermissions.contains(BULK_EDIT_ITEM_POST_PERMISSION);
    log.info("isBulkEditReadPermissionExists:: user {} has read permissions {} for {} in tenant {}", folioExecutionContext.getUserId(),
      isReadPermissionsExist, entityType, tenantId);
    return isReadPermissionsExist;
  }
}
