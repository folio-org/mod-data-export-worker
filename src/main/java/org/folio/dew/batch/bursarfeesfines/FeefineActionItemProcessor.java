package org.folio.dew.batch.bursarfeesfines;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.folio.des.domain.dto.BursarFeeFines;
import org.folio.des.domain.dto.BursarFeeFinesTypeMapping;
import org.folio.dew.batch.ExecutionContextUtils;
import org.folio.dew.batch.bursarfeesfines.service.BursarFeesFinesUtils;
import org.folio.dew.domain.dto.Account;
import org.folio.dew.domain.dto.Feefineaction;
import org.folio.dew.domain.dto.bursarfeesfines.BursarFormat;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class FeefineActionItemProcessor implements ItemProcessor<Feefineaction, BursarFormat> {

  private Map<String, String> userIdMap;
  private BursarFeeFines bursarFeeFines;
  private List<Account> accounts;

  @Override
  public BursarFormat process(Feefineaction item) {
    var format = new BursarFormat();
    format.setEmployeeId(BursarFeesFinesUtils.getEmployeeId(item.getUserId(), userIdMap));
    format.setAmount(BursarFeesFinesUtils.normalizeAmount(item.getAmountAction()));
    format.setTransactionDate(BursarFeesFinesUtils.getTransactionDate(item.getDateAction()));
    format.setSfs("SFS");
    format.setTermValue("    ");

    var account = getAccount(item);
    BursarFeeFinesTypeMapping mapping = account == null ? null : BursarFeesFinesUtils.getMapping(bursarFeeFines, account);
    format.setItemType(BursarFeesFinesUtils.formatItemType(mapping == null ? null : mapping.getItemType()));
    format.setDescription(
        BursarFeesFinesUtils.formatItemTypeDescription(mapping == null ? item.getTypeAction() : mapping.getItemDescription()));

    return format;
  }

  @BeforeStep
  public void initStep(StepExecution stepExecution) {
    Map<String, String> externalIdMap = (Map<String, String>) ExecutionContextUtils.getExecutionVariable(stepExecution,
        "userIdMap");
    userIdMap = externalIdMap == null ? Collections.emptyMap() : externalIdMap;

    bursarFeeFines = (BursarFeeFines) ExecutionContextUtils.getExecutionVariable(stepExecution, "bursarFeeFines");
    accounts = (List<Account>) ExecutionContextUtils.getExecutionVariable(stepExecution, "accounts");
  }

  private Account getAccount(Feefineaction feefineaction) {
    return accounts.stream().filter(a -> a.getId().equals(feefineaction.getAccountId())).findFirst().orElse(null);
  }

}
