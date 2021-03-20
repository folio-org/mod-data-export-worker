package org.folio.dew.batch.bursarfeesfines;

import org.folio.dew.batch.bursarfeesfines.service.BursarFeesFinesUtils;
import org.folio.dew.domain.dto.Feefineaction;
import org.folio.dew.domain.dto.bursarfeesfines.BursarFormat;
import org.folio.dew.utils.ExecutionContextUtils;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Component
@StepScope
public class FeefineActionItemProcessor implements ItemProcessor<Feefineaction, BursarFormat> {

  private Map<String, String> userIdMap;

  @Override
  public BursarFormat process(Feefineaction item) {
    BursarFormat format = new BursarFormat();
    format.setEmployeeId(BursarFeesFinesUtils.getEmployeeId(item.getUserId(), userIdMap));
    format.setAmount(BursarFeesFinesUtils.normalizeAmount(item.getAmountAction()));
    format.setTransactionDate(BursarFeesFinesUtils.getTransactionDate(item.getDateAction()));
    format.setSfs("SFS");
    format.setTermValue("    ");
    format.setDescription(BursarFeesFinesUtils.getItemTypeDescription(item.getTypeAction()));
    format.setItemType(BursarFeesFinesUtils.getItemType());
    return format;
  }

  @BeforeStep
  public void initStep(StepExecution stepExecution) {
    var externalIdMap = ExecutionContextUtils.getExecutionVariable(stepExecution, "userIdMap");
    this.userIdMap = externalIdMap == null ? Collections.emptyMap() : (Map<String, String>) externalIdMap;
  }

}
