package org.folio.dew.service;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.des.config.KafkaConfiguration;
import org.folio.des.domain.JobParameterNames;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.JobCommand;
import org.folio.dew.BaseBatchTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.kafka.support.Acknowledgment;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JobCommandsReceiverServiceTest extends BaseBatchTest {

  @Test
  @DisplayName("Start job by kafka request")
  void startJobTest() throws JobExecutionException {
    doNothing().when(acknowledgment).acknowledge();

    UUID id = UUID.randomUUID();
    JobCommand jobCommand = createStartJobRequest(id);

    service.onMessage(
        new ConsumerRecord<>(KafkaConfiguration.Topic.JOB_COMMAND.getName(), 0, 0, jobCommand.getId().toString(), jobCommand),
        acknowledgment);

    verify(exportJobManager, times(1)).launchJob(any());

    final Acknowledgment savedAcknowledgment = repository.getAcknowledgement(id.toString());

    assertNotNull(savedAcknowledgment);
  }

  @Test
  @DisplayName("Delete files by kafka request")
  void deleteFilesTest() {
    doNothing().when(acknowledgment).acknowledge();

    UUID id = UUID.randomUUID();
    JobCommand jobCommand = createDeleteJobRequest(id);

    service.onMessage(
        new ConsumerRecord<>(KafkaConfiguration.Topic.JOB_COMMAND.getName(), 0, 0, jobCommand.getId().toString(), jobCommand),
        acknowledgment);

    verify(acknowledgment, times(1)).acknowledge();
  }

  private JobCommand createStartJobRequest(UUID id) {
    JobCommand jobCommand = new JobCommand();
    jobCommand.setType(JobCommand.Type.START);
    jobCommand.setId(id);
    jobCommand.setName(ExportType.CIRCULATION_LOG.toString());
    jobCommand.setDescription("Start job test desc");
    jobCommand.setExportType(ExportType.CIRCULATION_LOG);

    Map<String, JobParameter> params = new HashMap<>();
    params.put("query", new JobParameter(""));
    jobCommand.setJobParameters(new JobParameters(params));
    return jobCommand;
  }

  private JobCommand createDeleteJobRequest(UUID id) {
    JobCommand jobCommand = new JobCommand();
    jobCommand.setType(JobCommand.Type.DELETE);
    jobCommand.setId(id);
    jobCommand.setJobParameters(
        new JobParameters(Collections.singletonMap(JobParameterNames.OUTPUT_FILES_IN_STORAGE, new JobParameter(""))));
    return jobCommand;
  }

}
