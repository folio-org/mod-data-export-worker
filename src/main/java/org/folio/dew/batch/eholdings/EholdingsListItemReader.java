package org.folio.dew.batch.eholdings;

import java.util.List;
import org.folio.dew.batch.ExecutionContextUtils;
import org.folio.dew.domain.dto.EHoldingsResourceExportFormat;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class EholdingsListItemReader implements ItemReader<EHoldingsResourceExportFormat> {

  private List<EHoldingsResourceExportFormat> list;

  @BeforeStep
  public void beforeStep(StepExecution stepExecution) {
    list = (List<EHoldingsResourceExportFormat>) ExecutionContextUtils.getExecutionVariable(stepExecution, "holdings");
  }

  @Override
  public EHoldingsResourceExportFormat read() throws Exception {
    if (!list.isEmpty()) {
      return list.remove(0);
    }
    return null;
  }
}
