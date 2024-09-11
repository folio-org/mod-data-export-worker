package org.folio.dew.batch.bulkedit.jobs.permissions.check;

import org.folio.dew.domain.dto.EntityType;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequiredPermissionResolverTest {

  @Test
  void testGetReadPermission() {
    var requiredPermissionResolver = new RequiredPermissionResolver();
    assertEquals(Set.of("ui-users.view"),  requiredPermissionResolver.getReadPermission(EntityType.USER));
    assertEquals(Set.of("ui-inventory.item.edit"),  requiredPermissionResolver.getReadPermission(EntityType.ITEM));
    assertEquals(Set.of("ui-inventory.holdings.edit"),  requiredPermissionResolver.getReadPermission(EntityType.HOLDINGS_RECORD));
    assertEquals(Set.of("ui-inventory.instance.view"),  requiredPermissionResolver.getReadPermission(EntityType.INSTANCE));
  }
}
