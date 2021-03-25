package org.folio.dew.batch.bursarfeesfines;

import org.folio.des.domain.dto.BursarFeeFines;
import org.folio.des.domain.dto.BursarFeeFinesTypeMapping;
import org.folio.dew.batch.bursarfeesfines.service.BursarFeesFinesUtils;
import org.folio.dew.domain.dto.Account;
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
public class AccountItemProcessor implements ItemProcessor<Account, BursarFormat> {

  private Map<String, String> userIdMap;
  private BursarFeeFines bursarFeeFines;

  @Override
  public BursarFormat process(Account item) {
    BursarFormat format = new BursarFormat();
    format.setEmployeeId(BursarFeesFinesUtils.getEmployeeId(item.getUserId(), userIdMap));
    format.setAmount(BursarFeesFinesUtils.normalizeAmount(item.getAmount()));
    format.setTransactionDate(BursarFeesFinesUtils.getTransactionDate(item.getMetadata().getCreatedDate()));
    format.setSfs("SFS");
    format.setTermValue("    ");

    BursarFeeFinesTypeMapping mapping = BursarFeesFinesUtils.getMapping(bursarFeeFines, item);
    format.setItemType(BursarFeesFinesUtils.formatItemType(mapping == null ? null : mapping.getItemType()));
    format.setDescription(
        BursarFeesFinesUtils.formatItemTypeDescription(mapping == null ? item.getFeeFineType() : mapping.getItemDescription()));

    return format;
  }

  @BeforeStep
  public void initStep(StepExecution stepExecution) {
    Map<String, String> externalIdMap = (Map<String, String>) stepExecution.getExecutionContext().get("userIdMap");
    userIdMap = externalIdMap == null ? Collections.emptyMap() : externalIdMap;

    bursarFeeFines = (BursarFeeFines) ExecutionContextUtils.getExecutionVariable(stepExecution, "bursarFeeFines");
  }

}
