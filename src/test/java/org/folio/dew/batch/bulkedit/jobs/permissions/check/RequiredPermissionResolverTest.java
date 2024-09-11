package org.folio.dew.batch.bulkedit.jobs.permissions.check;

import org.folio.dew.domain.dto.EntityType;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequiredPermissionResolverTest {

  @Test
  void testGetReadPermission() {
    var requiredPermissionResolver = new RequiredPermissionResolver();
    assertEquals(Set.of("ui-users.view", "ui-users.edit"),  requiredPermissionResolver.getReadPermission(EntityType.USER));
    assertEquals(Set.of("ui-inventory.item.edit", "ui-inventory.instance.view", "ui-inventory.item.create", "ui-inventory.item.delete"),  requiredPermissionResolver.getReadPermission(EntityType.ITEM));
    assertEquals(Set.of("ui-inventory.instance.view", "ui-inventory.holdings.edit", "ui-inventory.holdings.delete", "ui-inventory.holdings.create"),  requiredPermissionResolver.getReadPermission(EntityType.HOLDINGS_RECORD));
    assertEquals(Set.of("ui-inventory.instance.view", "ui-inventory.instance.create", "ui-inventory.instance.edit", "ui-quick-marc.quick-marc-editor.view", "ui-quick-marc.quick-marc-editor.all"),  requiredPermissionResolver.getReadPermission(EntityType.INSTANCE));
  }
}
