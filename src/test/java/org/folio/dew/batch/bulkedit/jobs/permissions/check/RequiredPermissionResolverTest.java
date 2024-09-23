package org.folio.dew.batch.bulkedit.jobs.permissions.check;

import org.folio.dew.domain.dto.EntityType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequiredPermissionResolverTest {

  @Test
  void testGetReadPermission() {
    var requiredPermissionResolver = new RequiredPermissionResolver();
    assertEquals("users.item.get",  requiredPermissionResolver.getReadPermission(EntityType.USER));
    assertEquals("inventory.items.item.get",  requiredPermissionResolver.getReadPermission(EntityType.ITEM));
    assertEquals("inventory-storage.holdings.item.get",  requiredPermissionResolver.getReadPermission(EntityType.HOLDINGS_RECORD));
    assertEquals("inventory.instances.item.get",  requiredPermissionResolver.getReadPermission(EntityType.INSTANCE));
  }
}
