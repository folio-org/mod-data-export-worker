package org.folio.dew.batch.bursarfeesfines;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.batch.ExecutionContextUtils;
import org.folio.dew.batch.bursarfeesfines.service.BursarExportUtils;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@StepScope
@RequiredArgsConstructor
public class FilenameTasklet implements Tasklet, StepExecutionListener {

  @Override
  public void beforeStep(StepExecution stepExecution) {
    String filename = BursarExportUtils.getFilename();
    log.info("Will produce output file with name {}", filename);

    ExecutionContextUtils.addToJobExecutionContext(
      stepExecution,
      "filename",
      filename,
      ""
    );
  }

  @Override
  public RepeatStatus execute(
    StepContribution contribution,
    ChunkContext chunkContext
  ) {
    return RepeatStatus.FINISHED;
  }
}
