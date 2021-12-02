package org.folio.dew.error;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.des.domain.dto.JobCommand;
import org.folio.dew.service.SaveErrorService;
import org.springframework.batch.core.annotation.OnSkipInProcess;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Log4j2
@RequiredArgsConstructor
@JobScope
@Component
public class BulkEditSkipListener {

  private final SaveErrorService saveErrorService;

  @Value("#{jobExecution.executionContext}")
  private final ExecutionContext executionContext;

  @OnSkipInProcess
  public void onSkipInProcess(JobCommand jobCommand, BulkEditException bulkEditException) {
    saveErrorService.saveErrorInCSV(jobCommand.getId().toString(), jobCommand.getIdentifierType().getValue(), bulkEditException, executionContext.getString("fileName"));
  }

}
