package org.folio.dew.batch.bulkedit.jobs.processidentifiers;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.folio.dew.domain.dto.JobParameterNames.JOB_ID;
import static org.folio.dew.utils.Constants.NUMBER_OF_PROCESSED_IDENTIFIERS;
import static org.folio.dew.utils.Constants.TOTAL_CSV_LINES;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.de.entity.Job;
import org.folio.dew.config.kafka.KafkaService;
import org.folio.dew.domain.dto.EntityType;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.Progress;
import org.folio.dew.service.BulkEditStatisticService;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class IdentifiersWriteListener<T> implements ItemWriteListener<T> {
  private final KafkaService kafka;

  @Value("#{stepExecution.jobExecution}")
  private JobExecution jobExecution;

  @Value("${application.chunks}")
  private int chunks;

  private final BulkEditStatisticService bulkEditStatisticService;

  @Override
  public void beforeWrite(Chunk<? extends T> list) {
    // do nothing
  }

  @Override
  public void afterWrite(Chunk<? extends T> list) {
    var job = new Job();
    job.setId(UUID.fromString(jobExecution.getJobParameters().getString(JOB_ID)));
    bulkEditStatisticService.incrementSuccess(job.getId().toString(), list.size());
    job.setType(ExportType.BULK_EDIT_IDENTIFIERS);
    job.setEntityType(EntityType.fromValue(jobExecution.getJobInstance().getJobName().split("-")[1]));
    job.setBatchStatus(BatchStatus.STARTED);
    job.setCreatedDate(new Date());
    job.setUpdatedDate(new Date());
    log.info("afterWrite:: update job by id {} after write for identifiers", job.getId());

    var totalCsvLines = jobExecution.getJobParameters().getLong(TOTAL_CSV_LINES);
    long processed = chunks;
    if (jobExecution.getExecutionContext().containsKey(NUMBER_OF_PROCESSED_IDENTIFIERS)) {
      processed += jobExecution.getExecutionContext().getLong(NUMBER_OF_PROCESSED_IDENTIFIERS);
    }
    if (nonNull(totalCsvLines) && processed > totalCsvLines) {
      processed = totalCsvLines;
    }
    jobExecution.getExecutionContext().putLong(NUMBER_OF_PROCESSED_IDENTIFIERS, processed);
    var progress = new Progress();
    progress.setTotal(isNull(totalCsvLines) ? 0 : totalCsvLines.intValue());
    progress.setProcessed((int) processed);
    progress.setProgress(isNull(totalCsvLines) ? 0 : calculateProgress(processed, totalCsvLines));
    progress.setSuccess(bulkEditStatisticService.getSuccess(job.getId().toString()));
    job.setProgress(progress);

    kafka.send(KafkaService.Topic.JOB_UPDATE, job.getId().toString(), job);
  }

  private int calculateProgress(long processed, long total) {
    if (total <= chunks) {
      return 90;
    }
    var res = (double) processed / total * 100;
    return (int) res - 10;
  }
}
