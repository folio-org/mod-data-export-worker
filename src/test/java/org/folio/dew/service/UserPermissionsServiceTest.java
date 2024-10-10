package org.folio.dew.service;

import org.folio.dew.batch.bulkedit.jobs.permissions.check.UserPermissions;
import org.folio.dew.client.EurekaUserPermissionsClient;
import org.folio.dew.client.OkapiUserPermissionsClient;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPermissionsServiceTest {

  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private OkapiUserPermissionsClient okapiUserPermissionsClient;
  @Mock
  private EurekaUserPermissionsClient eurekaUserPermissionsClient;

  @InjectMocks
  private UserPermissionsService userPermissionsService;

  @Test
  void getPermissionsTest() {
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());
    when(okapiUserPermissionsClient.getPermissions(isA(String.class))).thenReturn(new UserPermissions());

    userPermissionsService.setEurekaPermissionsModel(false);
    userPermissionsService.getPermissions();
    verify(okapiUserPermissionsClient).getPermissions(isA(String.class));
  }

  @Test
  void getPermissionsIfEurekaTest() {
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());
    when(eurekaUserPermissionsClient.getPermissions(isA(String.class), anyList())).thenReturn(new UserPermissions());

    userPermissionsService.setEurekaPermissionsModel(true);
    userPermissionsService.getPermissions();
    verify(eurekaUserPermissionsClient).getPermissions(isA(String.class), anyList());
  }
}
