package org.folio.dew.service;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.folio.dew.BaseBatchTest;
import org.folio.dew.client.UserTenantsClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class UserTenantsServiceCacheTest extends BaseBatchTest {

  @Autowired
  private UserTenantsService userTenantsService;

  @MockitoBean
  private UserTenantsClient userTenantsClient;

  @BeforeAll
  static void beforeAll() {
    setUpTenant(NON_CONSORTIUM_TENANT);
  }

  @Test
  void testGetCentralTenantForConsortiumTenantCaching() {
    var tenantId = CONSORTIUM_TENANT;

    var centralTenant = new UserTenantsClient.UserTenant(tenantId, "consortiumName");

    when(userTenantsClient.getUserTenants(tenantId)).thenReturn(
      new UserTenantsClient.UserTenants(singletonList(centralTenant)));

    // First call - should invoke the client
    var consortiumCentralTenantResult1 = userTenantsService.getCentralTenant(tenantId);
    // Second call - should use cache, not invoke the client again
    var consortiumCentralTenantResult2 = userTenantsService.getCentralTenant(tenantId);

    assertTrue(consortiumCentralTenantResult1.isPresent());
    assertTrue(consortiumCentralTenantResult2.isPresent());
    assertEquals(tenantId, consortiumCentralTenantResult1.get());
    assertEquals(tenantId, consortiumCentralTenantResult2.get());

    // Verify client is called only once due to caching
    verify(userTenantsClient, times(1)).getUserTenants(tenantId);
  }

  @Test
  void testGetCentralTenantForNonConsortiumTenantCaching() {
    var tenantId = NON_CONSORTIUM_TENANT;

    when(userTenantsClient.getUserTenants(tenantId)).thenReturn(
      new UserTenantsClient.UserTenants(emptyList()));

    // First call - should invoke the client
    var consortiumCentralTenantResult1 = userTenantsService.getCentralTenant(tenantId);
    // Second call - should use cache, not invoke the client again
    var consortiumCentralTenantResult2 = userTenantsService.getCentralTenant(tenantId);

    assertFalse(consortiumCentralTenantResult1.isPresent());
    assertFalse(consortiumCentralTenantResult2.isPresent());

    // Verify client is called only once due to caching
    verify(userTenantsClient, times(1)).getUserTenants(tenantId);
  }
}
