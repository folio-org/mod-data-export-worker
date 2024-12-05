package org.folio.dew.batch.bulkedit.jobs.permissions.check;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.client.EurekaUserPermissionsClient;
import org.folio.dew.client.OkapiUserPermissionsClient;
import org.folio.dew.service.FolioExecutionContextManager;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.folio.dew.batch.bulkedit.jobs.permissions.check.PermissionEnum.BULK_EDIT_INVENTORY_VIEW_PERMISSION;
import static org.folio.dew.batch.bulkedit.jobs.permissions.check.PermissionEnum.BULK_EDIT_USERS_VIEW_PERMISSION;
import static org.folio.dew.batch.bulkedit.jobs.permissions.check.PermissionEnum.INVENTORY_INSTANCES_ITEM_GET_PERMISSION;
import static org.folio.dew.batch.bulkedit.jobs.permissions.check.PermissionEnum.INVENTORY_ITEMS_ITEM_GET_PERMISSION;
import static org.folio.dew.batch.bulkedit.jobs.permissions.check.PermissionEnum.INVENTORY_STORAGE_HOLDINGS_ITEM_GET_PERMISSION;
import static org.folio.dew.batch.bulkedit.jobs.permissions.check.PermissionEnum.USER_ITEM_GET_PERMISSION;
import static org.folio.dew.utils.Constants.EUREKA_PLATFORM;

@Component
@RequiredArgsConstructor
public class PermissionsProvider extends FolioExecutionContextManager {

  @Setter
  @Value("${application.platform}")
  private String platform;

  private final FolioExecutionContext folioExecutionContext;
  private final OkapiUserPermissionsClient okapiUserPermissionsClient;
  private final EurekaUserPermissionsClient eurekaUserPermissionsClient;

  @Cacheable(cacheNames = "userPermissions")
  public List<String> getUserPermissions(String tenantId, String userId) {
    try (var ignored = new FolioExecutionContextSetter(refreshAndGetFolioExecutionContext(tenantId, folioExecutionContext))) {
      if (StringUtils.equals(EUREKA_PLATFORM, platform)) {
        var desiredPermissions = getDesiredPermissions();
        return eurekaUserPermissionsClient.getPermissions(userId,
          desiredPermissions).getPermissions();
      }
      return okapiUserPermissionsClient.getPermissions(userId).getPermissionNames();
    }
  }

  private List<String> getDesiredPermissions() {
    return List.of(BULK_EDIT_INVENTORY_VIEW_PERMISSION.getValue(), BULK_EDIT_USERS_VIEW_PERMISSION.getValue(), USER_ITEM_GET_PERMISSION.getValue(),
      INVENTORY_ITEMS_ITEM_GET_PERMISSION.getValue(), INVENTORY_STORAGE_HOLDINGS_ITEM_GET_PERMISSION.getValue(), INVENTORY_INSTANCES_ITEM_GET_PERMISSION.getValue());
  }
}
