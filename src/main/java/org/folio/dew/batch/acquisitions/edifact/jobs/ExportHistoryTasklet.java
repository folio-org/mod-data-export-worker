package org.folio.dew.batch.acquisitions.edifact.jobs;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.folio.dew.batch.ExecutionContextUtils;
import org.folio.dew.config.kafka.KafkaService;
import org.folio.dew.domain.dto.ExportHistory;
import org.json.JSONArray;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

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

    kafkaService.send(KafkaService.Topic.EXPORT_HISTORY_CREATE, null, exportHistory);

    return RepeatStatus.FINISHED;
  }

  ExportHistory buildExportHistory(ChunkContext chunkContext) {
    var stepExecutionContext = chunkContext.getStepContext().getStepExecution();
    var jobId = (String) chunkContext.getStepContext().getJobParameters().get("jobId");
    var polineIds = getPoLineIdsFromExecutionContext(stepExecutionContext);

    return new ExportHistory()
      .id(UUID.randomUUID().toString())
      .exportJobId(jobId)
      .exportDate(new Date())
      .exportType("EDIFACT")
      .exportedPoLineIds(polineIds);
  }

  List<String> getPoLineIdsFromExecutionContext(StepExecution stepExecutionContext) {
    JSONArray polineIds = new JSONArray((String) ExecutionContextUtils.getExecutionVariable(stepExecutionContext, "polineIds"));
    return polineIds.toList().stream()
      .map(String.class::cast)
      .collect(Collectors.toList());

  }
}
