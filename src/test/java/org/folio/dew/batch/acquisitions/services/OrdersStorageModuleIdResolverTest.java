package org.folio.dew.batch.acquisitions.services;

import org.folio.dew.client.EntitlementsClient;
import org.folio.dew.client.EntitlementsClient.Entitlement;
import org.folio.dew.client.EntitlementsClient.EntitlementCollection;
import org.folio.dew.client.TenantsClient;
import org.folio.dew.client.TenantsClient.Tenant;
import org.folio.dew.client.TenantsClient.TenantCollection;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.HttpServerErrorException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@ExtendWith(MockitoExtension.class)
class OrdersStorageModuleIdResolverTest {

  private static final String TENANT_NAME = "diku";
  private static final String TENANT_A = "0665fa8b-529e-4d5d-9cb3-74d5cfd6c8c2";
  private static final String TENANT_B = "11111111-1111-1111-1111-111111111111";
  private static final String OS_MODULE_ID = "mod-orders-storage-14.0.0-SNAPSHOT.498";

  @Mock
  private EntitlementsClient entitlementsClient;
  @Mock
  private TenantsClient tenantsClient;
  @Mock
  private FolioExecutionContext folioExecutionContext;

  @InjectMocks
  private OrdersStorageModuleIdResolver resolver;

  @Test
  void resolve_singleTenantResponse_picksModuleWithoutTenantLookup() {
    when(entitlementsClient.getEntitlements(true, 500)).thenReturn(new EntitlementCollection(List.of(
      entitlement(TENANT_A, "app-acq-1.0.0", List.of("mod-orders-13.1.0-SNAPSHOT.1121", OS_MODULE_ID)),
      entitlement(TENANT_A, "app-users-1.0.0", List.of("mod-users-19.6.0-SNAPSHOT.379")))));

    assertThat(resolver.resolve()).isEqualTo(OS_MODULE_ID);
    verify(tenantsClient, never()).getTenants(anyString(), anyInt());
  }

  @Test
  void resolve_crossTenantResponse_filtersToCurrentTenantByName() {
    when(entitlementsClient.getEntitlements(true, 500)).thenReturn(new EntitlementCollection(List.of(
      entitlement(TENANT_B, "app-acq-1.0.0", List.of("mod-orders-storage-99.0.0-OTHER.1")),
      entitlement(TENANT_A, "app-acq-1.0.0", List.of(OS_MODULE_ID)))));
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_NAME);
    when(tenantsClient.getTenants("name==" + TENANT_NAME, 1))
      .thenReturn(new TenantCollection(List.of(new Tenant(TENANT_A, TENANT_NAME))));

    assertThat(resolver.resolve()).isEqualTo(OS_MODULE_ID);
  }

  @Test
  void resolve_crossTenant_tenantLookupEmpty_returnsNull() {
    when(entitlementsClient.getEntitlements(true, 500)).thenReturn(new EntitlementCollection(List.of(
      entitlement(TENANT_B, "app", List.of("mod-orders-storage-99.0.0-OTHER.1")),
      entitlement(TENANT_A, "app", List.of(OS_MODULE_ID)))));
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_NAME);
    when(tenantsClient.getTenants(anyString(), anyInt())).thenReturn(new TenantCollection(List.of()));

    assertThat(resolver.resolve()).isNull();
  }

  @Test
  void resolve_noOrdersStorageModule_returnsNull() {
    when(entitlementsClient.getEntitlements(true, 500)).thenReturn(new EntitlementCollection(List.of(
      entitlement(TENANT_A, "app", List.of("mod-users-19.6.0-SNAPSHOT.379")))));

    assertThat(resolver.resolve()).isNull();
  }

  @Test
  void resolve_emptyEntitlements_returnsNull() {
    when(entitlementsClient.getEntitlements(true, 500)).thenReturn(new EntitlementCollection(List.of()));

    assertThat(resolver.resolve()).isNull();
  }

  @Test
  void resolve_entitlementsClientThrows_returnsNull() {
    when(entitlementsClient.getEntitlements(true, 500))
      .thenThrow(HttpServerErrorException.create(INTERNAL_SERVER_ERROR, "boom", null, null, null));

    assertThat(resolver.resolve()).isNull();
  }

  @Test
  void resolve_tenantClientThrows_returnsNull() {
    when(entitlementsClient.getEntitlements(true, 500)).thenReturn(new EntitlementCollection(List.of(
      entitlement(TENANT_B, "app", List.of("mod-orders-storage-99.0.0-OTHER.1")),
      entitlement(TENANT_A, "app", List.of(OS_MODULE_ID)))));
    lenient().when(folioExecutionContext.getTenantId()).thenReturn(TENANT_NAME);
    when(tenantsClient.getTenants(anyString(), anyInt()))
      .thenThrow(HttpServerErrorException.create(INTERNAL_SERVER_ERROR, "boom", null, null, null));

    assertThat(resolver.resolve()).isNull();
  }

  private static Entitlement entitlement(String tenantId, String applicationId, List<String> modules) {
    return new Entitlement(tenantId, applicationId, modules);
  }
}
