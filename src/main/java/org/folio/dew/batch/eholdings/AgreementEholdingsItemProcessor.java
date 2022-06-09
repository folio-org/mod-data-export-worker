package org.folio.dew.batch.eholdings;

import static org.folio.dew.client.AgreementClient.getFiltersParam;

import lombok.extern.log4j.Log4j2;
import org.folio.dew.client.AgreementClient;
import org.folio.dew.domain.dto.EHoldingsResourceExportFormat;
import org.springframework.batch.item.ItemProcessor;

@Log4j2
public class AgreementEholdingsItemProcessor
  implements ItemProcessor<EHoldingsResourceExportFormat, EHoldingsResourceExportFormat> {

  private final AgreementClient agreementClient;
  private final EHoldingsToExportFormatMapper mapper;
  private final boolean loadPackageAgreements;
  private final boolean loadResourceAgreements;

  public AgreementEholdingsItemProcessor(AgreementClient agreementClient, EHoldingsToExportFormatMapper mapper,
                                         boolean loadPackageAgreements, boolean loadResourceAgreements) {
    this.agreementClient = agreementClient;
    this.mapper = mapper;
    this.loadPackageAgreements = loadPackageAgreements;
    this.loadResourceAgreements = loadResourceAgreements;
  }

  @Override
  public EHoldingsResourceExportFormat process(EHoldingsResourceExportFormat exportFormat) throws Exception {
    if (loadPackageAgreements) {
      var packageId = exportFormat.getPackageId();
      var agreements = agreementClient.getAssignedAgreements(getFiltersParam(packageId));
      if (agreements.size() > 0) {
        exportFormat.setPackageAgreements(mapper.convertAgreements(agreements));
      }
    }
    if (loadResourceAgreements) {
      var resourceId = exportFormat.getPackageId() + "-" + exportFormat.getTitleId();
      var agreements = agreementClient.getAssignedAgreements(getFiltersParam(resourceId));
      if (agreements.size() > 0) {
        exportFormat.setTitleAgreements(mapper.convertAgreements(agreements));
      }
    }
    return exportFormat;
  }

}
