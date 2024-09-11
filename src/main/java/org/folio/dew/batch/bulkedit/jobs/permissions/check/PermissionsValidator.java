package org.folio.dew.batch.bulkedit.jobs.permissions.check;

import lombok.RequiredArgsConstructor;
import org.folio.dew.domain.dto.EntityType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PermissionsValidator {

  private final PermissionsProvider permissionsProvider;
  private final RequiredPermissionResolver requiredPermissionResolver;

  public boolean isBulkEditReadPermissionExists(String tenantId, EntityType entityType) {
    var readPermissionForEntity = requiredPermissionResolver.getReadPermission(entityType);
    var desiredPermissions = permissionsProvider.getDesiredPermissions(tenantId);
    var isReadPermissionExist = desiredPermissions.stream().filter(readPermissionForEntity::contains).findFirst();
    return isReadPermissionExist.isPresent();
  }
}
