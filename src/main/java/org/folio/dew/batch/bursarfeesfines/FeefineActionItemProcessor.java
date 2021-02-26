package org.folio.dew.batch.bursarfeesfines;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.domain.dto.Feefineaction;
import org.folio.dew.domain.dto.bursarfeesfines.BursarFormat;
import org.folio.dew.utils.ExecutionContextUtils;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class FeefineActionItemProcessor implements ItemProcessor<Feefineaction, BursarFormat> {

  private Map<String, String> userIdMap;

  @Override
  public BursarFormat process(Feefineaction item) {
    BursarFormat format = new BursarFormat();
    format.setEmployeeId(getEmployeeId(item.getUserId()));
    format.setAmount(normalizeAmount(item.getAmountAction()));
    format.setTransactionDate(getTransactionDate(item.getDateAction()));
    format.setSfs("SFS");
    format.setTermValue("    ");
    format.setDescription(getItemTypeDescription(item.getTypeAction()));
    format.setItemType(getItemType());
    return format;
  }

  private String getItemType() {
    return StringUtils.rightPad("", 12);
  }

  private String getItemTypeDescription(String feeFineType) {
    return StringUtils.rightPad(feeFineType, 30);
  }

  private String normalizeAmount(BigDecimal amount) {
    return StringUtils.leftPad(amount.toString(), 9, "0");
  }

  private String getTransactionDate(Date createdDate) {
    final SimpleDateFormat dateFormat = new SimpleDateFormat("MMddyy");
    return dateFormat.format(createdDate);
  }

  private String getEmployeeId(String userId) {
    String externalId = userIdMap.get(userId);
    externalId = externalId == null ? "" : externalId.substring(externalId.length() - 7);
    return StringUtils.rightPad(externalId, 11);
  }

  @BeforeStep
  public void initStep(StepExecution stepExecution) {
    var externalIdMap = ExecutionContextUtils.getExecutionVariable(stepExecution, "userIdMap");
    this.userIdMap = externalIdMap == null ? Collections.emptyMap() : (Map<String, String>) externalIdMap;
  }
}
