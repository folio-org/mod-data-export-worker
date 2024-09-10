package org.folio.dew.batch.bulkedit.jobs.permissions.check;

import org.folio.dew.domain.dto.EntityType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequiredPermissionResolverTest {

  @Test
  void testGetReadPermission() {
    var requiredPermissionResolver = new RequiredPermissionResolver();
    assertEquals("ui-users.view",  requiredPermissionResolver.getReadPermission(EntityType.USER));
    assertEquals("ui-inventory.item.edit",  requiredPermissionResolver.getReadPermission(EntityType.ITEM));
    assertEquals("ui-inventory.holdings.edit",  requiredPermissionResolver.getReadPermission(EntityType.HOLDINGS_RECORD));
    assertEquals("ui-inventory.instance.view",  requiredPermissionResolver.getReadPermission(EntityType.INSTANCE));
  }
}
