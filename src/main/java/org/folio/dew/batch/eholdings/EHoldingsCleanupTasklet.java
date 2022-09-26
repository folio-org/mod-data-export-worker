package org.folio.dew.batch.eholdings;

import org.folio.dew.repository.EHoldingsPackageRepository;
import org.folio.dew.repository.EHoldingsResourceRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@JobScope
public class EHoldingsCleanupTasklet implements Tasklet, StepExecutionListener {

  @Value("#{jobExecution}")
  private JobExecution jobExecution;

  private Long jobId;

  private final EHoldingsPackageRepository packageRepository;
  private final EHoldingsResourceRepository resourceRepository;

  public EHoldingsCleanupTasklet(EHoldingsPackageRepository packageRepository,
                                 EHoldingsResourceRepository resourceRepository) {
    this.packageRepository = packageRepository;
    this.resourceRepository = resourceRepository;
  }

  @Override
  public RepeatStatus execute(@NotNull StepContribution stepContribution, @NotNull ChunkContext chunkContext) {
    packageRepository.deleteAllByJobExecutionId(jobId);
    resourceRepository.deleteAllByJobExecutionId(jobId);
    return RepeatStatus.FINISHED;
  }

  @Override
  public void beforeStep(@NotNull StepExecution stepExecution) {
    jobId = jobExecution.getJobId();
  }

  @Override
  public ExitStatus afterStep(@NotNull StepExecution stepExecution) {
    return ExitStatus.COMPLETED;
  }
}
