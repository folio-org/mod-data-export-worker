package org.folio.dew.batch.bulkedit.jobs.permissions.check;

import org.folio.dew.domain.dto.EntityType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequiredPermissionResolverTest {

  @Test
  void testGetReadPermission() {
    var requiredPermissionResolver = new RequiredPermissionResolver();
    assertEquals("bulk-edit.users.get",  requiredPermissionResolver.getReadPermission(EntityType.USER));
    assertEquals("bulk-edit.items.get",  requiredPermissionResolver.getReadPermission(EntityType.ITEM));
    assertEquals("bulk-edit.holdings.get",  requiredPermissionResolver.getReadPermission(EntityType.HOLDINGS_RECORD));
    assertEquals("bulk-edit.instances.get",  requiredPermissionResolver.getReadPermission(EntityType.INSTANCE));
  }
}
