package org.folio.dew.batch.bulkedit.jobs.permissions.check;

import lombok.RequiredArgsConstructor;
import org.folio.dew.domain.dto.EntityType;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Component;

import static org.folio.dew.batch.bulkedit.jobs.permissions.check.PermissionEnum.BULK_EDIT_INVENTORY_VIEW_PERMISSION;
import static org.folio.dew.batch.bulkedit.jobs.permissions.check.PermissionEnum.BULK_EDIT_USERS_VIEW_PERMISSION;

@Component
@RequiredArgsConstructor
public class PermissionsValidator {

  private final PermissionsProvider permissionsProvider;
  private final RequiredPermissionResolver requiredPermissionResolver;
  private final FolioExecutionContext folioExecutionContext;

  public boolean isBulkEditReadPermissionExists(String tenantId, EntityType entityType) {
    var readPermissionForEntity = requiredPermissionResolver.getReadPermission(entityType);
    var userPermissions = permissionsProvider.getUserPermissions(tenantId, folioExecutionContext.getUserId().toString());
    var isReadPermissionsExist = false;
    if (entityType == EntityType.USER) {
      isReadPermissionsExist = userPermissions.contains(readPermissionForEntity.getValue()) && userPermissions.contains(BULK_EDIT_USERS_VIEW_PERMISSION.getValue());
    } else {
      isReadPermissionsExist = userPermissions.contains(readPermissionForEntity.getValue()) && userPermissions.contains(BULK_EDIT_INVENTORY_VIEW_PERMISSION.getValue());
    }
    return isReadPermissionsExist;
  }
}
