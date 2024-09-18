package org.folio.dew.batch.bulkedit.jobs.permissions.check;


import org.folio.dew.domain.dto.EntityType;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.folio.dew.batch.bulkedit.jobs.permissions.check.PermissionsValidator.BULK_EDIT_ITEM_POST_PERMISSION;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionsValidatorTest {

  @Mock
  private PermissionsProvider permissionsProvider;
  @Mock
  private RequiredPermissionResolver requiredPermissionResolver;
  @Mock
  private FolioExecutionContext folioExecutionContext;

  @InjectMocks
  private PermissionsValidator permissionsValidator;

  @Test
  void testIsBulkEditReadPermissionExists() {
    when(permissionsProvider.getUserPermissions("tenant1")).thenReturn(List.of("read_permission", "not_read_permission", BULK_EDIT_ITEM_POST_PERMISSION));
    when(permissionsProvider.getUserPermissions("tenant2")).thenReturn(List.of("not_read_permission"));
    when(requiredPermissionResolver.getReadPermission(EntityType.ITEM)).thenReturn("read_permission");
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());

    assertTrue(permissionsValidator.isBulkEditReadPermissionExists("tenant1", EntityType.ITEM));
    assertFalse(permissionsValidator.isBulkEditReadPermissionExists("tenant2", EntityType.ITEM));
  }
}
