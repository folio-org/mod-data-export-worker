package org.folio.dew.batch.bulkedit.jobs.permissions.check;


import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum PermissionEnum {
  BULK_EDIT_INVENTORY_VIEW_PERMISSION("bulk-operations.item.inventory.get"),
  BULK_EDIT_USERS_VIEW_PERMISSION("bulk-operations.item.users.get"),
  USER_ITEM_GET_PERMISSION("users.item.get"),
  INVENTORY_ITEMS_ITEM_GET_PERMISSION("inventory.items.item.get"),
  INVENTORY_STORAGE_HOLDINGS_ITEM_GET_PERMISSION("inventory-storage.holdings.item.get"),
  INVENTORY_INSTANCES_ITEM_GET_PERMISSION("inventory.instances.item.get");

  private final String value;
}
