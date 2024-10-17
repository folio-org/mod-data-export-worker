package org.folio.dew.batch.bulkedit.jobs.permissions.check;


import org.folio.dew.domain.dto.EntityType;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.folio.dew.batch.bulkedit.jobs.permissions.check.PermissionEnum.BULK_EDIT_INVENTORY_VIEW_PERMISSION;
import static org.folio.dew.batch.bulkedit.jobs.permissions.check.PermissionEnum.BULK_EDIT_USERS_VIEW_PERMISSION;
import static org.folio.dew.batch.bulkedit.jobs.permissions.check.PermissionEnum.INVENTORY_ITEMS_ITEM_GET_PERMISSION;
import static org.folio.dew.batch.bulkedit.jobs.permissions.check.PermissionEnum.USER_ITEM_GET_PERMISSION;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionsValidatorTest {

  @Mock
  private PermissionsProvider permissionsProvider;
  @Spy
  private RequiredPermissionResolver requiredPermissionResolver;
  @Mock
  private FolioExecutionContext folioExecutionContext;

  @InjectMocks
  private PermissionsValidator permissionsValidator;

  @Test
  void testIsBulkEditReadPermissionExistsForInventoryRecords() {
    when(permissionsProvider.getUserPermissions(eq("tenant1"), any())).thenReturn(List.of(INVENTORY_ITEMS_ITEM_GET_PERMISSION.getValue(), "not_read_permission", BULK_EDIT_INVENTORY_VIEW_PERMISSION.getValue()));
    when(permissionsProvider.getUserPermissions(eq("tenant2"), any())).thenReturn(List.of("not_read_permission"));
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());

    assertTrue(permissionsValidator.isBulkEditReadPermissionExists("tenant1", EntityType.ITEM));
    assertFalse(permissionsValidator.isBulkEditReadPermissionExists("tenant2", EntityType.ITEM));
  }

  @Test
  void testIsBulkEditReadPermissionExistsForUsers() {
    when(permissionsProvider.getUserPermissions(eq("tenant1"), any())).thenReturn(List.of(USER_ITEM_GET_PERMISSION.getValue(), "not_read_permission", BULK_EDIT_USERS_VIEW_PERMISSION.getValue(),  BULK_EDIT_USERS_VIEW_PERMISSION.getValue()));
    when(permissionsProvider.getUserPermissions(eq("tenant2"), any())).thenReturn(List.of("not_read_permission"));
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());

    assertTrue(permissionsValidator.isBulkEditReadPermissionExists("tenant1", EntityType.USER));
    assertFalse(permissionsValidator.isBulkEditReadPermissionExists("tenant2", EntityType.USER));
  }
}
