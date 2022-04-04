package org.folio.dew.batch.bulkedit.jobs.updatejob.listeners;

import static org.folio.dew.domain.dto.JobParameterNames.JOB_ID;
import static org.folio.dew.domain.dto.JobParameterNames.TOTAL_RECORDS;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.folio.de.entity.Job;
import org.folio.dew.config.kafka.KafkaService;
import org.folio.dew.domain.dto.EntityType;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.Progress;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@JobScope
@RequiredArgsConstructor
public class UpdateRecordWriteListener<T> implements ItemWriteListener<T> {

  private static final int BATCH_SIZE = 10;

  private final KafkaService kafka;

  @Value("#{jobExecution}")
  private JobExecution jobExecution;
  private AtomicLong processedRecords = new AtomicLong();

  @Override
  public void beforeWrite(List<? extends T> items) {
    // do nothing
  }

  @Override
  public void afterWrite(List<? extends T> items) {
    var job = prepareJobWithProgress();
    kafka.send(KafkaService.Topic.JOB_UPDATE, job.getId().toString(), job);
  }

  @Override
  public void onWriteError(Exception exception, List<? extends T> items) {
    // do nothing
  }

  private Job prepareJobWithProgress() {
    long totalRecords = jobExecution.getExecutionContext().getLong(TOTAL_RECORDS);
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
    progress.setTotal((int) totalRecords);
    progress.setProcessed((int) processedRecords.get());
    progress.setProgress((int) getProgressBarValue(processedRecords.get(), totalRecords));
    job.setProgress(progress);
    return job;
  }

  private long getProgressBarValue(long processed, long totalRecords) {
    if (totalRecords < BATCH_SIZE) {
      return 100;
    }
    return processed / totalRecords * 100;
  }
}
