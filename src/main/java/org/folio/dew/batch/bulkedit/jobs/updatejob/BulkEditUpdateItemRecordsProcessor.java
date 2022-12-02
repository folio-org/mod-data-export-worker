package org.folio.dew.batch.bulkedit.jobs.updatejob;

import static org.folio.dew.utils.Constants.FILE_NAME;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.dew.domain.dto.Item;
import org.folio.dew.domain.dto.ItemFormat;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.service.BulkEditParseService;
import org.folio.dew.service.BulkEditProcessingErrorsService;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@Qualifier("updateItemRecordsProcessor")
@RequiredArgsConstructor
@JobScope
public class BulkEditUpdateItemRecordsProcessor implements ItemProcessor<ItemFormat, Item> {

  @Value("#{jobParameters['jobId']}")
  private String jobId;

  @Value("#{jobParameters['identifierType']}")
  private String identifierType;

  @Value("#{jobExecution}")
  private JobExecution jobExecution;

  private final BulkEditParseService bulkEditParseService;
  private final BulkEditProcessingErrorsService bulkEditProcessingErrorsService;

  @Override
  public Item process(ItemFormat itemFormat) throws Exception {
    try {
      return bulkEditParseService.mapItemFormatToItem(itemFormat);
    } catch (Exception e) {
      log.error("Error process item format {} : {}",  itemFormat.getIdentifier(identifierType), e.getMessage());
      bulkEditProcessingErrorsService.saveErrorInCSV(jobId, itemFormat.getIdentifier(identifierType), new BulkEditException(e.getMessage()), FilenameUtils.getName(jobExecution.getJobParameters().getString(FILE_NAME)));
      return null;
    }
  }

}
