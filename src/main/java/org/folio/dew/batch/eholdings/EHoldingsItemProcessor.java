package org.folio.dew.batch.eholdings;

import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import org.folio.dew.domain.dto.EHoldingsPackageExportFormat;

@Log4j2
@Component
@StepScope
public class EHoldingsItemProcessor implements ItemProcessor<EHoldingsPackageExportFormat, EHoldingsPackageExportFormat> {

  @Override
  public EHoldingsPackageExportFormat process(EHoldingsPackageExportFormat eHoldingsRecord) throws Exception {
    return eHoldingsRecord;
  }
}
