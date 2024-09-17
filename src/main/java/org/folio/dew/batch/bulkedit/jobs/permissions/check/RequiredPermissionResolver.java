package org.folio.dew.batch.bulkedit.jobs.permissions.check;

import org.folio.dew.domain.dto.EntityType;
import org.springframework.stereotype.Component;

@Component
public class RequiredPermissionResolver {

  public String getReadPermission(EntityType entityType) {
    return switch (entityType) {
      case USER -> "users.item.get";
      case ITEM -> "inventory.items.item.get";
      case HOLDINGS_RECORD -> "inventory-storage.holdings.item.get";
      case INSTANCE -> "inventory.instances.item.get";
    };
  }
}
