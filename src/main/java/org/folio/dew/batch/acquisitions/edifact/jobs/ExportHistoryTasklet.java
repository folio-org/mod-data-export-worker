package org.folio.dew.batch.acquisitions.edifact.jobs;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.folio.dew.batch.ExecutionContextUtils;
import org.folio.dew.config.kafka.KafkaService;
import org.folio.dew.domain.dto.ExportHistory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@Component
@StepScope
@Log4j2
public class ExportHistoryTasklet implements Tasklet {

  private final KafkaService kafkaService;
  private final ObjectMapper objectMapper;
  @Value("#{jobParameters['jobId']}")
  private String jobId;
  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    var exportHistory = buildExportHistory(chunkContext);

    kafkaService.send(KafkaService.Topic.EXPORT_HISTORY_CREATE, null, exportHistory);

    return RepeatStatus.FINISHED;
  }

  ExportHistory buildExportHistory(ChunkContext chunkContext) {
    var stepExecutionContext = chunkContext.getStepContext().getStepExecution();
    var polineIds = getPoLineIdsFromExecutionContext(stepExecutionContext);

    return new ExportHistory()
      .id(UUID.randomUUID().toString())
      .exportJobId(jobId)
      .exportDate(new Date())
      .exportType("EDIFACT")
      .exportedPoLineIds(polineIds);
  }

  List<String> getPoLineIdsFromExecutionContext(StepExecution stepExecutionContext) {
    try {
      return objectMapper.readValue((String) ExecutionContextUtils.getExecutionVariable(stepExecutionContext, "polineIds"), new TypeReference<>() {});
    } catch (Exception e) {
      return Collections.emptyList();
    }
  }
}
