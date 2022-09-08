package org.folio.dew.batch.bulkedit.jobs.updatejob;

import static org.folio.dew.utils.Constants.FILE_NAME;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.dew.domain.dto.HoldingsFormat;
import org.folio.dew.domain.dto.HoldingsRecord;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.service.BulkEditProcessingErrorsService;
import org.folio.dew.service.mapper.HoldingsMapper;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@Qualifier("updateHoldingsRecordsProcessor")
@RequiredArgsConstructor
@JobScope
public class BulkEditUpdateHoldingsRecordsProcessor implements ItemProcessor<HoldingsFormat, HoldingsRecord> {

  @Value("#{jobParameters['jobId']}")
  private String jobId;

  @Value("#{jobParameters['identifierType']}")
  private String identifierType;

  @Value("#{jobExecution}")
  private JobExecution jobExecution;

  private final HoldingsMapper holdingsMapper;
  private final BulkEditProcessingErrorsService bulkEditProcessingErrorsService;

  @Override
  public HoldingsRecord process(HoldingsFormat holdingsFormat) throws Exception {
    try {
      return holdingsMapper.mapToHoldingsRecord(holdingsFormat);
    } catch (Exception e) {
      log.error("Error process holdings format {} : {}",  holdingsFormat.getIdentifier(identifierType), e.getMessage());
      bulkEditProcessingErrorsService.saveErrorInCSV(jobId, holdingsFormat.getIdentifier(identifierType), new BulkEditException(e.getMessage()), FilenameUtils.getName(jobExecution.getJobParameters().getString(FILE_NAME)));
      return null;
    }
  }

}
