package org.folio.dew.batch.bulkedit.jobs.permissions.check;

import org.folio.dew.domain.dto.EntityType;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class RequiredPermissionResolver {

  public Set<String> getReadPermission(EntityType entityType) {
    return switch (entityType) {
      case USER -> Set.of("ui-users.view", "ui-users.edit");
      case ITEM -> Set.of("ui-inventory.item.edit", "ui-inventory.item.create", "ui-inventory.item.delete", "ui-inventory.instance.view");
      case HOLDINGS_RECORD -> Set.of("ui-inventory.holdings.edit", "ui-inventory.holdings.create", "ui-inventory.holdings.delete",
        "ui-inventory.instance.view");
      case INSTANCE -> Set.of("ui-inventory.instance.create", "ui-inventory.instance.view", "ui-inventory.instance.edit",
        "ui-quick-marc.quick-marc-editor.view", "ui-quick-marc.quick-marc-editor.all");
    };
  }
}
