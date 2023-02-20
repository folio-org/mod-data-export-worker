package org.folio.dew.batch.bursarfeesfines;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
// import org.folio.dew.domain.dto.BursarFeeFinesTypeMapping;
import org.folio.dew.batch.ExecutionContextUtils;
import org.folio.dew.batch.bursarfeesfines.service.BursarExportService;
import org.folio.dew.batch.bursarfeesfines.service.BursarFeesFinesUtils;
import org.folio.dew.domain.dto.Account;
import org.folio.dew.domain.dto.Feefineaction;
import org.folio.dew.domain.dto.bursarfeesfines.BursarFormat;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@StepScope
@RequiredArgsConstructor
public class FeefineActionItemProcessor implements ItemProcessor<Feefineaction, BursarFormat> {

  private final BursarExportService exportService;
  private Map<String, String> userIdMap;
  private List<Account> accounts;
  @Value("#{jobParameters['jobId']}")
  private String jobId;

  @Override
  public BursarFormat process(Feefineaction item) {
    var format = new BursarFormat();
    format.setEmployeeId(BursarFeesFinesUtils.getEmployeeId(item.getUserId(), userIdMap));
    format.setAmount(BursarFeesFinesUtils.normalizeAmount(item.getAmountAction()));
    // format.setTransactionDate(BursarFeesFinesUtils.getTransactionDate(item.getDateAction()));
    format.setSfs("SFS");
    format.setTermValue("    ");

    var account = getAccount(item);
    // BursarFeeFinesTypeMapping mapping = account == null ? null : exportService.getMapping(jobId, account);
    // format.setItemType(BursarFeesFinesUtils.formatItemType(mapping == null ? null : mapping.getItemType()));
    // format.setDescription(
    //     BursarFeesFinesUtils.formatItemTypeDescription(mapping == null ? item.getTypeAction() : mapping.getItemDescription()));

    return format;
  }

  @BeforeStep
  public void initStep(StepExecution stepExecution) {
    Map<String, String> externalIdMap =
        (Map<String, String>) ExecutionContextUtils.getExecutionVariable(stepExecution, "userIdMap");
    userIdMap = externalIdMap == null ? Collections.emptyMap() : externalIdMap;

    accounts = (List<Account>) ExecutionContextUtils.getExecutionVariable(stepExecution, "accounts");
  }

  private Account getAccount(Feefineaction feefineaction) {
    return accounts.stream().filter(a -> a.getId().equals(feefineaction.getAccountId())).findFirst().orElse(null);
  }

}
