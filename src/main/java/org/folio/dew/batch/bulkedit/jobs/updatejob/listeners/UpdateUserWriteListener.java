package org.folio.dew.batch.bulkedit.jobs.updatejob.listeners;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.folio.de.entity.Job;
import org.folio.dew.config.kafka.KafkaService;
import org.folio.dew.domain.dto.EntityType;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.Progress;
import org.folio.dew.domain.dto.User;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import static org.folio.dew.domain.dto.JobParameterNames.JOB_ID;
import static org.folio.dew.domain.dto.JobParameterNames.TOTAL_USERS;

@Component
@JobScope
@RequiredArgsConstructor
public class UpdateUserWriteListener implements ItemWriteListener<User> {

  private static final int BATCH_SIZE = 10;

  private final KafkaService kafka;

  @Value("#{jobExecution}")
  private JobExecution jobExecution;
  private AtomicLong processedUsers = new AtomicLong();

  @Override
  public void beforeWrite(List<? extends User> items) {
  }

  @Override
  public void afterWrite(List<? extends User> items) {
    var job = prepareJobWithProgress();
    kafka.send(KafkaService.Topic.JOB_UPDATE, job.getId().toString(), job);
  }

  @Override
  public void onWriteError(Exception exception, List<? extends User> items) {
  }

  private Job prepareJobWithProgress() {
    long totalUsers = jobExecution.getExecutionContext().getLong(TOTAL_USERS);
    if (totalUsers < BATCH_SIZE) {
      processedUsers.addAndGet(totalUsers);
    } else {
      processedUsers.addAndGet(BATCH_SIZE);
    }
    var job = new Job();
    job.setId(UUID.fromString(jobExecution.getJobParameters().getString(JOB_ID)));
    job.setType(ExportType.BULK_EDIT_UPDATE);
    job.setEntityType(EntityType.USER);
    job.setBatchStatus(BatchStatus.STARTED);
    job.setStartTime(new Date());
    job.setCreatedDate(new Date());
    job.setEndTime(new Date());
    job.setUpdatedDate(new Date());

    Progress progress = new Progress();
    progress.setTotal((int) totalUsers);
    progress.setProcessed((int) processedUsers.get());
    progress.setProgress((int) getProgressBarValue(processedUsers.get(), totalUsers));
    job.setProgress(progress);
    return job;
  }

  private long getProgressBarValue(long processed, long totalUsers) {
    if (totalUsers < BATCH_SIZE) {
      return 100;
    }
    return processed / totalUsers * 100;
  }
}
