package org.folio.dew.batch.bursarfeesfines;

import java.util.Collections;
import java.util.Map;
import org.folio.dew.domain.dto.Account;
import org.folio.dew.domain.dto.bursarfeesfines.BursarFormat;
import org.folio.dew.utils.BursarFeesFinesUtils;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class AccountItemProcessor implements ItemProcessor<Account, BursarFormat> {

  private Map<String, String> userIdMap;

  @Override
  public BursarFormat process(Account item) {
    BursarFormat format = new BursarFormat();
    format.setEmployeeId(BursarFeesFinesUtils.getEmployeeId(item.getUserId(), userIdMap));
    format.setAmount(BursarFeesFinesUtils.normalizeAmount(item.getAmount()));
    format.setTransactionDate(
        BursarFeesFinesUtils.getTransactionDate(item.getMetadata().getCreatedDate()));
    format.setSfs("SFS");
    format.setTermValue("    ");
    format.setDescription(BursarFeesFinesUtils.getItemTypeDescription(item.getFeeFineType()));
    format.setItemType(BursarFeesFinesUtils.getItemType());
    return format;
  }

  @BeforeStep
  public void initStep(StepExecution stepExecution) {
    var externalIdMap = stepExecution.getExecutionContext().get("userIdMap");
    this.userIdMap =
        externalIdMap == null ? Collections.emptyMap() : (Map<String, String>) externalIdMap;
  }
}
