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
  private static final String TENANT_ID = "testTenant";

  @Mock
  private UserTenantsClient userTenantsClient;

  @InjectMocks
  private UserTenantsService userTenantsService;

  @Test
  void testGetCentralTenantWithBlankTenantId() {
    var centralTenant = userTenantsService.getCentralTenant("");

    assertTrue(centralTenant.isEmpty());
    verifyNoInteractions(userTenantsClient);
  }

  @Test
  void testGetCentralTenantWithNullResponse() {
    when(userTenantsClient.getUserTenants(TENANT_ID)).thenReturn(null);

    var centralTenant = userTenantsService.getCentralTenant(TENANT_ID);

    assertTrue(centralTenant.isEmpty());
    verify(userTenantsClient).getUserTenants(TENANT_ID);
  }

  @Test
  void testGetCentralTenantWithNoUserTenants() {
    when(userTenantsClient.getUserTenants(TENANT_ID)).thenReturn(new UserTenantsClient.UserTenants(emptyList()));

    var centralTenant = userTenantsService.getCentralTenant(TENANT_ID);

    assertTrue(centralTenant.isEmpty());
    verify(userTenantsClient).getUserTenants(TENANT_ID);
  }

  @Test
  void testGetCentralTenantForConsortiumMemberTenant() {
    var tenantId = "centralTenant";
    var consortiumIdValue = "consortium";

    var centralTenant = new UserTenantsClient.UserTenant(tenantId, consortiumIdValue);

    when(userTenantsClient.getUserTenants(tenantId)).thenReturn(
      new UserTenantsClient.UserTenants(singletonList(centralTenant)));

    var consortiumCentralTenant = userTenantsService.getCentralTenant(tenantId);

    assertTrue(consortiumCentralTenant.isPresent());
    assertEquals(tenantId, consortiumCentralTenant.get());
    verify(userTenantsClient).getUserTenants(tenantId);
  }
}

