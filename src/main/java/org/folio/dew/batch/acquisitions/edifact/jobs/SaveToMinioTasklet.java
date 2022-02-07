package org.folio.dew.batch.acquisitions.edifact.jobs;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@Component
@StepScope
@Log4j2
public class SaveToMinioTasklet implements Tasklet {
  @Override public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    // TODO: implement minio upload
    return RepeatStatus.FINISHED;
  }
}
