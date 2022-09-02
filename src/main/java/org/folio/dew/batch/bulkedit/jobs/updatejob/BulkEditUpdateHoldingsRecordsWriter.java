package org.folio.dew.batch.bulkedit.jobs.updatejob;

import static org.folio.dew.utils.Constants.FILE_NAME;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.dew.client.HoldingClient;
import org.folio.dew.domain.dto.HoldingsRecord;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.service.BulkEditProcessingErrorsService;
import org.folio.dew.service.BulkEditStatisticService;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Log4j2
@Component
@Qualifier("updateHoldingsRecordsWriter")
@RequiredArgsConstructor
@JobScope
public class BulkEditUpdateHoldingsRecordsWriter implements ItemWriter<HoldingsRecord> {

  @Value("#{jobParameters['jobId']}")
  private String jobId;
  @Value("#{jobExecution}")
  private JobExecution jobExecution;

  private final HoldingClient holdingClient;
  private final BulkEditProcessingErrorsService bulkEditProcessingErrorsService;
  private final BulkEditStatisticService bulkEditStatisticService;

  @Override
  public void write(List<? extends HoldingsRecord> holdingsRecords) throws Exception {
    holdingsRecords.forEach(record -> {
      try {
        holdingClient.updateHoldingsRecord(record, record.getId());
        bulkEditStatisticService.incrementSuccess();
        log.info("Update holdings record with id - {} by job id {}", record.getId(), jobId);
      } catch (Exception e) {
        log.info("Cannot update holdings record with id {}. Reason: {}",  record.getId(), e.getMessage());
        bulkEditProcessingErrorsService.saveErrorInCSV(jobId, record.getId(), new BulkEditException(e.getMessage()), FilenameUtils.getName(jobExecution.getJobParameters().getString(FILE_NAME)));
      }
    });
  }
}
