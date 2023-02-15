package org.folio.dew.batch.bulkedit.jobs.updatejob;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.dew.client.InventoryClient;
import org.folio.dew.domain.dto.Item;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.service.BulkEditProcessingErrorsService;
import org.folio.dew.service.BulkEditStatisticService;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static org.folio.dew.utils.Constants.FILE_NAME;

@Log4j2
@Component
@Qualifier("updateItemRecordsWriter")
@RequiredArgsConstructor
@JobScope
public class BulkEditUpdateItemRecordsWriter implements ItemWriter<Item> {

  @Value("#{jobParameters['jobId']}")
  private String jobId;
  @Value("#{jobExecution}")
  private JobExecution jobExecution;

  private final InventoryClient inventoryClient;
  private final BulkEditProcessingErrorsService bulkEditProcessingErrorsService;
  private final BulkEditStatisticService bulkEditStatisticService;

  @Override
  public void write(Chunk<? extends Item> items) throws Exception {
    items.forEach(item -> {
      try {
        inventoryClient.updateItem(item, item.getId());
        bulkEditStatisticService.incrementSuccess();
        log.info("Update item with id - {} by job id {}", item.getId(), jobId);
      } catch (Exception e) {
        log.info("Cannot update item with id {}. Reason: {}",  item.getId(), e.getMessage());
        bulkEditProcessingErrorsService.saveErrorInCSV(jobId, item.getId(), new BulkEditException(e.getMessage()), FilenameUtils.getName(jobExecution.getJobParameters().getString(FILE_NAME)));
      }
    });
  }
}
