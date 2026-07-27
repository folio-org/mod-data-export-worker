package org.folio.dew.batch.eholdings;

import org.folio.dew.domain.dto.eholdings.EHoldingsResourceDTO;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class EHoldingsAgreementItemProcessor
  implements ItemProcessor<EHoldingsResourceDTO, EHoldingsResourceDTO> {

  private final AgreementCache agreementCache;

  public EHoldingsAgreementItemProcessor(AgreementCache agreementCache) {
    this.agreementCache = agreementCache;
  }

  @Override
  public EHoldingsResourceDTO process(@NotNull EHoldingsResourceDTO eHoldingsResourceDTO) throws Exception {
    var attributes = eHoldingsResourceDTO.getResourcesData().getAttributes();
    var resourceId = attributes.getPackageId() + "-" + attributes.getTitleId();

    var agreements = agreementCache.get(resourceId);
    if (!agreements.isEmpty()) {
      eHoldingsResourceDTO.setAgreements(agreements);
    }
    return eHoldingsResourceDTO;
  }

}
