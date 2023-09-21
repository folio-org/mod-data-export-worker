package org.folio.dew.batch.bulkedit.jobs.updatejob.listeners;

import static org.folio.dew.domain.dto.JobParameterNames.JOB_ID;
import static org.folio.dew.domain.dto.JobParameterNames.TOTAL_RECORDS;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

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
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.item.Chunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@JobScope
@RequiredArgsConstructor
@Log4j2
public class UpdateRecordWriteListener<T> implements ItemWriteListener<T> {

  private static final int BATCH_SIZE = 10;

  private final KafkaService kafka;

  @Value("#{jobExecution}")
  private JobExecution jobExecution;

  private AtomicInteger processedRecords = new AtomicInteger();
  private final BulkEditStatisticService bulkEditUpdateStatisticService;

  @Override
  public void afterWrite(Chunk<? extends T> items) {
    var job = prepareJobWithProgress();
    kafka.send(KafkaService.Topic.JOB_UPDATE, job.getId().toString(), job);
  }

  private Job prepareJobWithProgress() {
    var totalRecords = jobExecution.getExecutionContext().getInt(TOTAL_RECORDS);
    if (totalRecords < BATCH_SIZE) {
      processedRecords.addAndGet(totalRecords);
    } else {
      processedRecords.addAndGet(BATCH_SIZE);
    }
    var job = new Job();
    job.setId(UUID.fromString(jobExecution.getJobParameters().getString(JOB_ID)));
    job.setType(ExportType.BULK_EDIT_UPDATE);
    job.setEntityType(EntityType.fromValue(jobExecution.getJobInstance().getJobName().split("-")[1]));
    job.setBatchStatus(BatchStatus.STARTED);
    job.setStartTime(new Date());
    job.setCreatedDate(new Date());
    job.setEndTime(new Date());
    job.setUpdatedDate(new Date());

    Progress progress = new Progress();
    progress.setTotal(totalRecords);
    progress.setProcessed(processedRecords.get());
    progress.setProgress(getProgressBarValue(processedRecords.get(), totalRecords));

    var statistic = bulkEditUpdateStatisticService.getStatistic();
    progress.setSuccess(statistic.getSuccess());
    job.setProgress(progress);
    return job;
  }

  private int getProgressBarValue(int processed, int totalRecords) {
    if (totalRecords < BATCH_SIZE) {
      return 90;
    }
    var progress = ((double) processed / totalRecords) * 100;
    return progress < 100 ? (int) progress : 99;
  }
}
