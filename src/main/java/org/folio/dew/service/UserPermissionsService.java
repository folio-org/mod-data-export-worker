package org.folio.dew.service;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.client.EurekaUserPermissionsClient;
import org.folio.dew.client.OkapiUserPermissionsClient;
import org.folio.spring.FolioExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;


@RequiredArgsConstructor
@Log4j2
@Service
public class UserPermissionsService {

  public static final String BULK_EDIT_INVENTORY_VIEW_PERMISSION = "bulk-operations.item.inventory.get";
  public static final String BULK_EDIT_USERS_VIEW_PERMISSION = "bulk-operations.item.users.get";

  public final static String USER_ITEM_GET_PERMISSION = "users.item.get";
  public final static String INVENTORY_ITEMS_ITEM_GET_PERMISSION = "inventory.items.item.get";
  public final static String INVENTORY_STORAGE_HOLDINGS_ITEM_GET_PERMISSION = "inventory-storage.holdings.item.get";
  public final static String INVENTORY_INSTANCES_ITEM_GET_PERMISSION = "inventory.instances.item.get";

  @Value("${application.eureka.permissions-model}")
  @Setter
  private boolean isEurekaPermissionsModel;

  private final FolioExecutionContext folioExecutionContext;
  private final OkapiUserPermissionsClient okapiUserPermissionsClient;
  private final EurekaUserPermissionsClient eurekaUserPermissionsClient;

  public List<String> getPermissions() {
    if (isEurekaPermissionsModel) {
      var desiredPermissions = getDesiredPermissions();
      return eurekaUserPermissionsClient.getPermissions(folioExecutionContext.getUserId().toString(),
        desiredPermissions).getPermissions();

    }
    return okapiUserPermissionsClient.getPermissions(folioExecutionContext.getUserId().toString()).getPermissionNames();
  }

  private List<String> getDesiredPermissions() {
    return List.of(BULK_EDIT_INVENTORY_VIEW_PERMISSION, BULK_EDIT_USERS_VIEW_PERMISSION, USER_ITEM_GET_PERMISSION,
      INVENTORY_ITEMS_ITEM_GET_PERMISSION, INVENTORY_STORAGE_HOLDINGS_ITEM_GET_PERMISSION, INVENTORY_INSTANCES_ITEM_GET_PERMISSION);
  }
}
