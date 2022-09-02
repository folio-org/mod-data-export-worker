package org.folio.dew.batch;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.repository.LocalFilesStorage;
import org.folio.dew.repository.RemoteFilesStorage;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;


@StepScope
@Log4j2
@RequiredArgsConstructor
@Getter
public abstract class BaseStepListener implements StepExecutionListener {

  private final RemoteFilesStorage remoteFilesStorage;
  private final LocalFilesStorage localFilesStorage;

  @Override
  public void beforeStep(StepExecution stepExecution) {
    // Nothing to do
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    var exitStatus = stepExecution.getExitStatus();
    if (ExitStatus.FAILED.getExitCode().equals(exitStatus.getExitCode())) {
      return exitStatus;
    }
    return afterStepExecution(stepExecution);
  }

  public abstract ExitStatus afterStepExecution(StepExecution stepExecution);

}
