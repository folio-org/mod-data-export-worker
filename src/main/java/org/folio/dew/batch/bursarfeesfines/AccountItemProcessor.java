package org.folio.dew.batch.bursarfeesfines;

import java.util.Collections;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.folio.dew.batch.bursarfeesfines.service.BursarExportService;
import org.folio.dew.batch.bursarfeesfines.service.BursarFeesFinesUtils;
import org.folio.dew.domain.dto.Account;
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
public class AccountItemProcessor implements ItemProcessor<Account, BursarFormat> {

  private final BursarExportService exportService;

  private Map<String, String> userIdMap;

  @Value("#{jobParameters['jobId']}")
  private String jobId;

  @Override
  public BursarFormat process(Account item) {
    var format = new BursarFormat();
    format.setEmployeeId(BursarFeesFinesUtils.getEmployeeId(item.getUserId(), userIdMap));
    format.setAmount(BursarFeesFinesUtils.normalizeAmount(item.getAmount()));
    format.setTransactionDate(BursarFeesFinesUtils.getTransactionDate(item.getMetadata().getCreatedDate()));
    format.setSfs("SFS");
    format.setTermValue("    ");

    // BursarFeeFinesTypeMapping mapping = exportService.getMapping(jobId, item);
    // format.setItemType(BursarFeesFinesUtils.formatItemType(mapping == null ? null : mapping.getItemType()));
    // format.setDescription(
    //     BursarFeesFinesUtils.formatItemTypeDescription(mapping == null ? item.getFeeFineType() : mapping.getItemDescription()));

    return format;
  }

  @BeforeStep
  public void initStep(StepExecution stepExecution) {
    Map<String, String> externalIdMap = (Map<String, String>) stepExecution.getExecutionContext().get("userIdMap");
    userIdMap = externalIdMap == null ? Collections.emptyMap() : externalIdMap;
  }

}
