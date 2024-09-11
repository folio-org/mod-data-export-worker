package org.folio.dew.batch.bulkedit.jobs.permissions.check;

import org.folio.dew.domain.dto.EntityType;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class RequiredPermissionResolver {

  public Set<String> getReadPermission(EntityType entityType) {
    return switch (entityType) {
      case USER -> Set.of("ui-users.view");
      case ITEM -> Set.of("ui-inventory.item.edit");
      case HOLDINGS_RECORD -> Set.of("ui-inventory.holdings.edit");
      case INSTANCE -> Set.of("ui-inventory.instance.view");
    };
  }
}
