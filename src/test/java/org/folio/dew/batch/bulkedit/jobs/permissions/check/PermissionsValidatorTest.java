package org.folio.dew.batch.bulkedit.jobs.permissions.check;


import org.folio.dew.domain.dto.EntityType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionsValidatorTest {

  @Mock
  private PermissionsProvider permissionsProvider;
  @Mock
  private RequiredPermissionResolver requiredPermissionResolver;
  @InjectMocks
  private PermissionsValidator permissionsValidator;

  @Test
  void testIsBulkEditReadPermissionExists() {
    when(permissionsProvider.getDesiredPermissions("tenant1")).thenReturn(List.of("read_permission", "not_read_permission"));
    when(permissionsProvider.getDesiredPermissions("tenant2")).thenReturn(List.of("not_read_permission"));
    when(requiredPermissionResolver.getReadPermission(EntityType.ITEM)).thenReturn(Set.of("read_permission"));

    assertTrue(permissionsValidator.isBulkEditReadPermissionExists("tenant1", EntityType.ITEM));
    assertFalse(permissionsValidator.isBulkEditReadPermissionExists("tenant2", EntityType.ITEM));
  }
}
