package org.folio.dew.batch.bursarfeesfines;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.batch.ExecutionContextUtils;
import org.folio.dew.batch.bursarfeesfines.service.BursarExportUtils;
import org.folio.dew.domain.dto.BursarExportJob;
import org.springframework.batch.core.ExitStatus;
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
public class PrepareContextTasklet implements Tasklet, StepExecutionListener {

  private final ObjectMapper objectMapper;
  private boolean isAggregateJob;

  @Override
  public void beforeStep(StepExecution stepExecution) {
    String filename = BursarExportUtils.getFilename();
    log.info("Will produce output file with name {}", filename);

    ExecutionContextUtils.addToJobExecutionContext(stepExecution, "filename", filename, "");

    try {
      BursarExportJob jobConfig = objectMapper.readValue(stepExecution.getJobParameters()
        .getString("bursarFeeFines"), BursarExportJob.class);
      stepExecution.getJobExecution()
        .getExecutionContext()
        .put("jobConfig", jobConfig);

      isAggregateJob = jobConfig.getGroupByPatron();
    } catch (JsonProcessingException e) {
      log.error("Could not parse job config... ", e);
    }
  }

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    if (!isAggregateJob) {
      chunkContext.getStepContext()
        .getStepExecution()
        .setExitStatus(new ExitStatus("NOT AGGREGATE"));
    } else {
      log.info("Is aggregate job");
      chunkContext.getStepContext()
        .getStepExecution()
        .setExitStatus(new ExitStatus("IS AGGREGATE"));
    }
    return RepeatStatus.FINISHED;
  }
}
