package org.folio.dew.batch.eholdings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.folio.dew.client.AgreementClient;
import org.folio.dew.client.dto.Entitlements;
import org.folio.dew.config.properties.EHoldingsJobProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TitleAgreementLineLoaderTest {

  @Mock
  private AgreementClient agreementClient;
  @Mock
  private EHoldingsJobProperties jobProperties;

  private AgreementCache agreementCache;
  private TitleAgreementLineLoader loader;

  @BeforeEach
  void setUp() {
    agreementCache = new AgreementCache();
    lenient().when(jobProperties.getEntitlementsPerPage()).thenReturn(2);
    loader = new TitleAgreementLineLoader(agreementClient, jobProperties, agreementCache);
  }

  @Test
  void shouldSkipWhenNoEntitlementsFound() {
    when(agreementClient.getEntitlements(eq(filter("28-5642")),
      eq(false), eq(true), anyInt(), eq(1)))
      .thenReturn(emptyResponse());

    loader.loadTitleAgreementLines("28-5642");

    assertThat(agreementCache.get("28-5642-123")).isEmpty();
    verify(agreementClient, times(1)).getEntitlements(filter("28-5642"), false, true, 2, 1);
    verify(agreementClient, never()).getEntitlements(filter("28-5642"), false, true, 2, 2);
  }

  @Test
  void shouldGroupByReferenceAndConvertAgreement() {
    when(agreementClient.getEntitlements(filter("1-22"), false, true, 2, 1))
      .thenReturn(response(2, 1, entitlement("1-22-333", "Agreement A", "Active"),
        entitlement("1-22-444", "Agreement B", "Draft")));

    loader.loadTitleAgreementLines("1-22");

    var agreements333 = agreementCache.get("1-22-333");
    assertThat(agreements333).hasSize(1);
    AgreementClient.Agreement a = agreements333.getFirst();
    assertThat(a.getName()).isEqualTo("Agreement A");
    assertThat(a.getStatus()).isEqualTo("Active");
    assertThat(a.getStartDate()).isEqualTo("2022-06-01");

    var agreements444 = agreementCache.get("1-22-444");
    assertThat(agreements444).hasSize(1);
    assertThat(agreements444.getFirst().getStatus()).isEqualTo("Draft");
  }

  @Test
  void shouldPaginateThroughAllPages() {
    when(agreementClient.getEntitlements(filter("1-22"), false, true, 2, 1))
      .thenReturn(response(5, 3,
        entitlement("1-22-1", "A", "Active"),
        entitlement("1-22-2", "B", "Active")));
    when(agreementClient.getEntitlements(filter("1-22"), false, true, 2, 2))
      .thenReturn(response(5, 3,
        entitlement("1-22-3", "C", "Active"),
        entitlement("1-22-4", "D", "Active")));
    when(agreementClient.getEntitlements(filter("1-22"), false, true, 2, 3))
      .thenReturn(response(5, 3,
        entitlement("1-22-5", "E", "Active")));

    loader.loadTitleAgreementLines("1-22");

    assertThat(agreementCache.get("1-22-1")).hasSize(1);
    assertThat(agreementCache.get("1-22-2")).hasSize(1);
    assertThat(agreementCache.get("1-22-3")).hasSize(1);
    assertThat(agreementCache.get("1-22-4")).hasSize(1);
    assertThat(agreementCache.get("1-22-5")).hasSize(1);

    // pages 1, 2, 3 – no page 4
    verify(agreementClient, times(3))
      .getEntitlements(eq(filter("1-22")), eq(false), eq(true), eq(2), anyInt());
  }

  @Test
  void shouldSetFetchExternalResourcesToFalse() {
    when(agreementClient.getEntitlements(filter("1-22"), false, true, 2, 1))
      .thenReturn(emptyResponse());

    loader.loadTitleAgreementLines("1-22");

    ArgumentCaptor<Boolean> fetchCaptor = ArgumentCaptor.forClass(Boolean.class);
    verify(agreementClient).getEntitlements(
      eq(filter("1-22")), fetchCaptor.capture(), anyBoolean(), anyInt(), anyInt());
    assertThat(fetchCaptor.getValue()).isFalse();
  }

  @Test
  void shouldBuildContainsFilterWithPackageIdAndAuthority() {
    assertThat(AgreementClient.getEntitlementsFilterParam("28-5642"))
      .isEqualTo("reference=~28-5642-&&authority=EKB-TITLE");
  }

  private String filter(String packageId) {
    return AgreementClient.getEntitlementsFilterParam(packageId);
  }

  private Entitlements emptyResponse() {
    var entitlements = new Entitlements();
    entitlements.setTotal(0);
    entitlements.setResults(List.of());
    return entitlements;
  }

  private Entitlements response(int total, int totalPages, Entitlements.Entitlement... items) {
    var entitlements = new Entitlements();
    entitlements.setTotal(total);
    entitlements.setTotalPages(totalPages);
    entitlements.setResults(List.of(items));
    return entitlements;
  }

  private Entitlements.Entitlement entitlement(String reference, String name, String statusLabel) {
    var entitlement = new Entitlements.Entitlement();
    entitlement.setReference(reference);
    var owner = new Entitlements.Owner();
    owner.setId("id-" + reference);
    owner.setName(name);
    owner.setStartDate("2022-06-01");
    owner.setAgreementStatus(Map.of("label", statusLabel, "value", statusLabel.toLowerCase()));
    entitlement.setOwner(owner);
    return entitlement;
  }
}
