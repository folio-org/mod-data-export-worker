package org.folio.dew.batch.bulkedit.jobs.permissions.check;

import org.folio.dew.domain.dto.EntityType;
import org.springframework.stereotype.Component;

import static org.folio.dew.service.UserPermissionsService.INVENTORY_INSTANCES_ITEM_GET_PERMISSION;
import static org.folio.dew.service.UserPermissionsService.INVENTORY_ITEMS_ITEM_GET_PERMISSION;
import static org.folio.dew.service.UserPermissionsService.INVENTORY_STORAGE_HOLDINGS_ITEM_GET_PERMISSION;
import static org.folio.dew.service.UserPermissionsService.USER_ITEM_GET_PERMISSION;

@Component
public class RequiredPermissionResolver {

  public String getReadPermission(EntityType entityType) {
    return switch (entityType) {
      case USER -> USER_ITEM_GET_PERMISSION;
      case ITEM -> INVENTORY_ITEMS_ITEM_GET_PERMISSION;
      case HOLDINGS_RECORD -> INVENTORY_STORAGE_HOLDINGS_ITEM_GET_PERMISSION;
      case INSTANCE -> INVENTORY_INSTANCES_ITEM_GET_PERMISSION;
    };
  }
}
