package org.folio.dew.batch.eholdings;

import static org.folio.dew.batch.ExecutionContextUtils.getExecutionVariable;
import static org.folio.dew.batch.eholdings.EHoldingsJobConstants.CONTEXT_PACKAGE;
import static org.folio.dew.batch.eholdings.EHoldingsJobConstants.CONTEXT_RESOURCES;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import org.folio.dew.batch.ExecutionContextUtils;
import org.folio.dew.domain.dto.eholdings.EHoldingsPackage;
import org.folio.dew.domain.dto.eholdings.EHoldingsResource;
import org.folio.dew.domain.dto.eholdings.EHoldingsResourceExportFormat;

@Component
@StepScope
@RequiredArgsConstructor
public class EHoldingsListItemReader implements ItemReader<EHoldingsResourceExportFormat> {

  private List<EHoldingsResourceExportFormat> list;
  private final EHoldingsToExportFormatMapper mapper;

  @BeforeStep
  public void beforeStep(StepExecution stepExecution) {
    var eHoldingsPackage = (EHoldingsPackage) getExecutionVariable(stepExecution, CONTEXT_PACKAGE);
    @SuppressWarnings("unchecked")
    var eHoldingsResources = (List<EHoldingsResource>) ExecutionContextUtils.getExecutionVariable(stepExecution, CONTEXT_RESOURCES);

    list = mapper.convertToExportFormat(eHoldingsPackage, eHoldingsResources);
  }

  @Override
  public EHoldingsResourceExportFormat read() throws Exception {
    if (!list.isEmpty()) {
      return list.remove(0);
    }
    return null;
  }
}
