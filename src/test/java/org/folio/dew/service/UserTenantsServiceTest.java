package org.folio.dew.service;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import org.folio.dew.client.UserTenantsClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserTenantsServiceTest {

  @Mock
  private UserTenantsClient userTenantsClient;

  @InjectMocks
  private UserTenantsService userTenantsService;

  @Test
  void testGetCentralTenantWithBlankTenantId() {
    var tenantId = "";

    var centralTenant = userTenantsService.getCentralTenant(tenantId);

    assertTrue(centralTenant.isEmpty());
    verifyNoInteractions(userTenantsClient);
  }

  @Test
  void testGetCentralTenantWithNullResponse() {
    var tenantId = "tenant1";

    when(userTenantsClient.getUserTenants(tenantId)).thenReturn(null);

    var centralTenant = userTenantsService.getCentralTenant(tenantId);

    assertTrue(centralTenant.isEmpty());
    verify(userTenantsClient).getUserTenants(tenantId);
  }

  @Test
  void testGetCentralTenantWithNoUserTenants() {
    var tenantId = "tenant1";

    when(userTenantsClient.getUserTenants(tenantId)).thenReturn(new UserTenantsClient.UserTenants(emptyList()));

    var centralTenant = userTenantsService.getCentralTenant(tenantId);

    assertTrue(centralTenant.isEmpty());
    verify(userTenantsClient).getUserTenants(tenantId);
  }

  @Test
  void testGetCentralTenantForMemverTenant() {
    var tenantId = "centralTenant";
    var consortiumIdValue = "consortium123";

    var centralTenant = new UserTenantsClient.UserTenant(tenantId, consortiumIdValue);

    when(userTenantsClient.getUserTenants(tenantId)).thenReturn(
      new UserTenantsClient.UserTenants(singletonList(centralTenant)));

    var consortiumCentralTenant = userTenantsService.getCentralTenant(tenantId);

    assertTrue(consortiumCentralTenant.isPresent());
    assertEquals(tenantId, consortiumCentralTenant.get());
    verify(userTenantsClient).getUserTenants(tenantId);
  }
}

