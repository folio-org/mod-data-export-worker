package org.folio.dew.batch.acquisitions.edifact.jobs;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.folio.dew.config.kafka.KafkaService;
import org.folio.dew.domain.dto.ExportHistory;
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
public class ExportHistoryTasklet implements Tasklet {

  private final KafkaService kafkaService;

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    var exportHistory = buildExportHistory(chunkContext);
    kafkaService.send(KafkaService.Topic.EXPORT_HISTORY_CREATE, exportHistory.getId(), exportHistory);

    return RepeatStatus.FINISHED;
  }

  ExportHistory buildExportHistory(ChunkContext chunkContext){
    List<String> polineIds = new ArrayList<>();
    return new ExportHistory()
      .id(UUID.randomUUID().toString())
      .exportJobId(chunkContext.getStepContext().getJobInstanceId().toString())
      .exportDate(new Date())
      .exportType("EDIFACT")
      .exportedPoLineIds(polineIds);
  }
}
