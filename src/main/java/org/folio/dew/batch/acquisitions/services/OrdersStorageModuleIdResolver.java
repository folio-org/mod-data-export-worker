package org.folio.dew.batch.acquisitions.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.client.EntitlementsClient;
import org.folio.dew.client.EntitlementsClient.Entitlement;
import org.folio.dew.client.TenantsClient;
import org.folio.dew.client.TenantsClient.Tenant;
import org.folio.spring.FolioExecutionContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolves the full, versioned module id of the {@code mod-orders-storage} module the current tenant
 * is entitled to (e.g. {@code mod-orders-storage-14.0.0-SNAPSHOT.498}). That id is required as the
 * {@code X-Okapi-Module-Id} header when calling the {@code interfaceType: multiple}
 * {@code custom-fields} interface, because the gateway routes such interfaces on the exact module id.
 *
 * <p>Multi-tenant safe: {@code /entitlements} is a cross-tenant manager endpoint, so the response may
 * carry entitlements for several tenants. When it does, the current tenant's id (UUID) is resolved by
 * name via {@code /tenants} and used to filter. When the response already contains a single tenant
 * (auto-scoped or single-tenant deployment) that step is skipped.
 *
 * <p>Degrades gracefully: any failure returns {@code null}, letting the caller send the email without
 * resolved custom-field tokens rather than failing the export.
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class OrdersStorageModuleIdResolver {

  private static final String MODULE_NAME = "mod-orders-storage";
  private static final String MODULE_ID_PREFIX = MODULE_NAME + "-";
  private static final int ENTITLEMENTS_LIMIT = 500;

  private final EntitlementsClient entitlementsClient;
  private final TenantsClient tenantsClient;
  private final FolioExecutionContext folioExecutionContext;

  @Cacheable(cacheNames = "ordersStorageModuleId", key = "@folioExecutionContext.tenantId", unless = "#result == null")
  public String resolve() {
    try {
      var entitlements = Optional.ofNullable(entitlementsClient.getEntitlements(true, ENTITLEMENTS_LIMIT))
        .map(EntitlementsClient.EntitlementCollection::entitlements)
        .orElseGet(List::of);
      if (entitlements.isEmpty()) {
        log.warn("resolve:: No entitlements returned - cannot resolve {} module id", MODULE_NAME);
        return null;
      }
      var tenantId = resolveTargetTenantId(entitlements);
      if (tenantId == null) {
        return null;
      }
      var moduleId = entitlements.stream()
        .filter(e -> tenantId.equals(e.tenantId()))
        .map(Entitlement::modules)
        .filter(Objects::nonNull)
        .flatMap(List::stream)
        .filter(Objects::nonNull)
        .filter(id -> id.startsWith(MODULE_ID_PREFIX))
        .findFirst()
        .orElse(null);
      if (moduleId == null) {
        log.warn("resolve:: Tenant '{}' has no entitled {} module", tenantId, MODULE_NAME);
      }
      return moduleId;
    } catch (RestClientException e) {
      log.warn("resolve:: Failed to resolve {} module id from entitlements", MODULE_NAME, e);
      return null;
    }
  }

  private String resolveTargetTenantId(List<Entitlement> entitlements) {
    var distinctTenantIds = entitlements.stream()
      .map(Entitlement::tenantId)
      .filter(Objects::nonNull)
      .distinct()
      .toList();
    if (distinctTenantIds.size() == 1) {
      // Auto-scoped by X-Okapi-Tenant, or a single-tenant deployment.
      return distinctTenantIds.get(0);
    }
    // Cross-tenant response: narrow to the current tenant by resolving its UUID from its name.
    return resolveCurrentTenantId();
  }

  private String resolveCurrentTenantId() {
    var tenantName = folioExecutionContext.getTenantId();
    if (StringUtils.isBlank(tenantName)) {
      log.warn("resolveCurrentTenantId:: No tenant in execution context");
      return null;
    }
    try {
      var tenantId = Optional.ofNullable(tenantsClient.getTenants("name==" + tenantName, 1))
        .map(TenantsClient.TenantCollection::tenants)
        .orElseGet(List::of).stream()
        .filter(t -> tenantName.equals(t.name()))
        .map(Tenant::id)
        .filter(StringUtils::isNotBlank)
        .findFirst()
        .orElse(null);
      if (tenantId == null) {
        log.warn("resolveCurrentTenantId:: Could not resolve id for tenant '{}'", tenantName);
      }
      return tenantId;
    } catch (RestClientException e) {
      log.warn("resolveCurrentTenantId:: Failed to resolve id for tenant '{}'", tenantName, e);
      return null;
    }
  }
}