package org.folio.dew.batch.bursarfeesfines;

import java.util.Collections;
import java.util.Map;
import org.folio.dew.domain.dto.Account;
import org.folio.dew.domain.dto.bursarfeesfines.BursarFormat;
import org.folio.dew.utils.FormatterUtils;
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
    format.setEmployeeId(FormatterUtils.getEmployeeId(item.getUserId(), userIdMap));
    format.setAmount(FormatterUtils.normalizeAmount(item.getAmount()));
    format.setTransactionDate(
        FormatterUtils.getTransactionDate(item.getMetadata().getCreatedDate()));
    format.setSfs("SFS");
    format.setTermValue("    ");
    format.setDescription(FormatterUtils.getItemTypeDescription(item.getFeeFineType()));
    format.setItemType(FormatterUtils.getItemType());
    return format;
  }

  @BeforeStep
  public void initStep(StepExecution stepExecution) {
    var externalIdMap = stepExecution.getExecutionContext().get("userIdMap");
    this.userIdMap =
        externalIdMap == null ? Collections.emptyMap() : (Map<String, String>) externalIdMap;
  }
}
