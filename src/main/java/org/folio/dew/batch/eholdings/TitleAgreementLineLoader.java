package org.folio.dew.batch.eholdings;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.client.AgreementClient;
import org.folio.dew.client.dto.Entitlements;
import org.folio.dew.config.properties.EHoldingsJobProperties;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class TitleAgreementLineLoader {

  private final AgreementClient agreementClient;
  private final EHoldingsJobProperties jobProperties;
  private final AgreementCache agreementCache;

  /**
   * Load all per-title agreement lines (entitlements) for the given package into cache.
   */
  public void loadTitleAgreementLines(String packageId) {
    log.trace("Loading title agreement lines for package {}...", packageId);
    var filters = AgreementClient.getEntitlementsFilterParam(packageId);
    int perPage = jobProperties.getEntitlementsPerPage();

    var response = agreementClient.getEntitlements(filters, false, true, perPage, 1);
    if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
      log.debug("No per-title entitlements for package {} - skipping agreement load", packageId);
      return;
    }

    loadToCache(response.getResults());

    int totalPages = response.getTotalPages() != null ? response.getTotalPages() : 1;
    for (int page = 2; page <= totalPages; page++) {
      var next = agreementClient.getEntitlements(filters, false, true, perPage, page);
      if (next != null && next.getResults() != null && !next.getResults().isEmpty()) {
        loadToCache(next.getResults());
      }
    }
    log.trace("Loaded title agreement lines for package {}", packageId);
  }

  private void loadToCache(List<Entitlements.Entitlement> entitlements) {
    for (var entitlement : entitlements) {
      var resourceId = entitlement.getReference();
      var owner = entitlement.getOwner();
      if (resourceId != null && owner != null) {
        agreementCache.put(resourceId, toAgreement(owner));
      }
    }
  }

  private AgreementClient.Agreement toAgreement(Entitlements.Owner owner) {
    var agreement = new AgreementClient.Agreement();
    agreement.setName(owner.getName());
    agreement.setStartDate(owner.getStartDate());
    if (owner.getAgreementStatus() != null) {
      agreement.setStatus(owner.getAgreementStatus().get("label"));
    }
    return agreement;
  }
}
