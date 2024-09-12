package org.folio.dew.batch.bulkedit.jobs.permissions.check;

import org.folio.dew.domain.dto.EntityType;
import org.springframework.stereotype.Component;

@Component
public class RequiredPermissionResolver {

  public String getReadPermission(EntityType entityType) {
    return switch (entityType) {
      case USER -> "bulk-edit.users.get";
      case ITEM -> "bulk-edit.items.get";
      case HOLDINGS_RECORD -> "bulk-edit.holdings.get";
      case INSTANCE -> "bulk-edit.instances.get";
    };
  }
}
