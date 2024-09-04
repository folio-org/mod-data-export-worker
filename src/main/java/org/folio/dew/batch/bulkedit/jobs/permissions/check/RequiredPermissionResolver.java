package org.folio.dew.batch.bulkedit.jobs.permissions.check;

import org.folio.dew.domain.dto.EntityType;
import org.springframework.stereotype.Component;

@Component
public class RequiredPermissionResolver {

  public String getReadPermission(EntityType entityType) {
    return switch (entityType) {
      case USER -> "ui-users.view";
      case ITEM -> "ui-inventory.item.edit";
      case HOLDINGS_RECORD -> "ui-inventory.holdings.edit";
      case INSTANCE -> "ui-inventory.instance.view";
    };
  }
}
