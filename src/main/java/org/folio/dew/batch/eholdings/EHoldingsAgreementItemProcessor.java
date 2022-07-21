package org.folio.dew.batch.eholdings;

import static org.folio.dew.batch.eholdings.EHoldingsJobConstants.LOAD_FIELD_TITLE_AGREEMENTS;
import static org.folio.dew.client.AgreementClient.getFiltersParam;

import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import org.folio.dew.client.AgreementClient;
import org.folio.dew.domain.dto.EHoldingsExportConfig;
import org.folio.dew.domain.dto.eholdings.EHoldingsResource;

@Component
@StepScope
public class EHoldingsAgreementItemProcessor
  implements ItemProcessor<EHoldingsResource, EHoldingsResource> {

  private final AgreementClient agreementClient;
  private final boolean loadResourceAgreements;

  public EHoldingsAgreementItemProcessor(AgreementClient agreementClient,
                                         EHoldingsExportConfig exportConfig) {
    this.agreementClient = agreementClient;
    this.loadResourceAgreements = exportConfig.getTitleFields() != null
      && exportConfig.getTitleFields().contains(LOAD_FIELD_TITLE_AGREEMENTS);
  }

  @Override
  public EHoldingsResource process(@NotNull EHoldingsResource eHoldingsResource) throws Exception {
    if (loadResourceAgreements) {
      var resourceDataAttributes = eHoldingsResource.getResourcesData().getAttributes();
      var resourceId = resourceDataAttributes.getPackageId() + "-" + resourceDataAttributes.getTitleId();
      var agreements = agreementClient.getAssignedAgreements(getFiltersParam(resourceId));
      if (!agreements.isEmpty()) {
        eHoldingsResource.setAgreements(agreements);
      }
    }
    return eHoldingsResource;
  }

}
