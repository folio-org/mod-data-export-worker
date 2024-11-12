package org.folio.dew.service;

import static org.folio.dew.utils.Constants.EUREKA_PLATFORM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.folio.dew.client.EurekaProxyTenantsClient;
import org.folio.dew.client.OkapiClient;
import org.folio.dew.domain.bean.ModuleForTenant;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class ModuleTenantServiceTest {

  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private OkapiClient okapiClient;
  @Mock
  private EurekaProxyTenantsClient eurekaProxyTenantsClient;

  @InjectMocks
  private ModuleTenantService moduleTenantService;

  @Test
  void getModuleIdForOkapiTest() {
    var tenantId = "diku";
    var moduleId = "moduleId";

    var module = new ModuleForTenant();
    module.setId(moduleId);
    when(folioExecutionContext.getTenantId()).thenReturn(tenantId);
    when(okapiClient.getModuleIds(isA(URI.class), eq(tenantId), eq("mod-users"))).thenReturn(List.of(module));

    var actual = moduleTenantService.getModUsersModuleId();

    verify(okapiClient).getModuleIds(isA(URI.class), eq(tenantId), eq("mod-users"));
    assertEquals(moduleId, actual);
  }

  @Test
  void getModuleIdForEurekaTest() {
    var tenantId = "diku";
    var moduleId = "mod-users-19.4.1-SNAPSHOT.322";

    var module1 = new ModuleForTenant();
    module1.setId(moduleId);
    var module2 = new ModuleForTenant();
    module2.setId("mod-users-bl-7.9.2-SNAPSHOT.170");
    var module3 = new ModuleForTenant();
    module3.setId("mod-users");

    when(folioExecutionContext.getTenantId()).thenReturn(tenantId);
    when(eurekaProxyTenantsClient.getModules(isA(URI.class), eq(tenantId))).thenReturn(List.of(module1, module2, module3));

    moduleTenantService.setPlatform(EUREKA_PLATFORM);
    var actual = moduleTenantService.getModUsersModuleId();

    verify(eurekaProxyTenantsClient).getModules(isA(URI.class), eq(tenantId));
    assertEquals(moduleId, actual);
  }
}
