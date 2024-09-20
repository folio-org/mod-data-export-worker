package org.folio.dew.batch.bulkedit.jobs.permissions.check;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.EntityType;
import org.folio.dew.exceptions.ReadPermissionDoesNotExist;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
public class PermissionsValidator {

  public static final String BULK_EDIT_INVENTORY_VIEW_PERMISSION = "bulk-operations.item.inventory.get";
  public static final String BULK_EDIT_USERS_VIEW_PERMISSION = "bulk-operations.item.users.get";

  private final PermissionsProvider permissionsProvider;
  private final RequiredPermissionResolver requiredPermissionResolver;
  private final FolioExecutionContext folioExecutionContext;

  public void checkBulkEditReadPermissions(String tenantId, EntityType entityType) {
    if (!isBulkEditReadPermissionExists(tenantId, entityType)) {
      throw new ReadPermissionDoesNotExist();
    }
  }

  public boolean isBulkEditReadPermissionExists(String tenantId, EntityType entityType) {
    var readPermissionForEntity = requiredPermissionResolver.getReadPermission(entityType);
    var userPermissions = permissionsProvider.getUserPermissions(tenantId);
    var isReadPermissionsExist = false;
    if (entityType == EntityType.USER) {
      isReadPermissionsExist = userPermissions.contains(readPermissionForEntity) && userPermissions.contains(BULK_EDIT_USERS_VIEW_PERMISSION);
    } else {
      isReadPermissionsExist = userPermissions.contains(readPermissionForEntity) && userPermissions.contains(BULK_EDIT_INVENTORY_VIEW_PERMISSION);
    }
    log.info("isBulkEditReadPermissionExists:: user {} has read permissions {} for {} in tenant {}", folioExecutionContext.getUserId(),
      isReadPermissionsExist, entityType, tenantId);
    return isReadPermissionsExist;
  }
}
