package org.folio.dew.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.kafka.support.Acknowledgment;

import org.folio.de.entity.JobCommand;
import org.folio.dew.BaseBatchTest;
import org.folio.dew.domain.dto.EHoldingsExportConfig;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.JobParameterNames;

class JobCommandsReceiverServiceTest extends BaseBatchTest {

  @Test
  @DisplayName("Start CirculationLog job by kafka request")
  void startCirculationLogJobTest() throws JobExecutionException {
    doNothing().when(acknowledgment).acknowledge();

    UUID id = UUID.randomUUID();
    JobCommand jobCommand = createStartCirculationLogJobRequest(id);

    jobCommandsReceiverService.receiveStartJobCommand(jobCommand, acknowledgment);

    verify(exportJobManagerSync, timeout(1_000)).launchJob(any());

    final Acknowledgment savedAcknowledgment = repository.getAcknowledgement(id.toString());

    assertNotNull(savedAcknowledgment);
  }

  @Test
  @DisplayName("Start EHoldings job by kafka request")
  void startEHoldingsJobTest() throws JobExecutionException {
    doNothing().when(acknowledgment).acknowledge();

    UUID id = UUID.randomUUID();
    JobCommand jobCommand = createStartEHoldingsJobRequest(id);

    jobCommandsReceiverService.receiveStartJobCommand(jobCommand, acknowledgment);

    verify(exportJobManagerSync, timeout(1_000)).launchJob(any());

    final Acknowledgment savedAcknowledgment = repository.getAcknowledgement(id.toString());

    assertNotNull(savedAcknowledgment);
  }

  @Test
  @DisplayName("Start EHoldings job with pausing/resuming consumer by kafka request")
  void runJobInNewThreadTest() throws JobExecutionException {
    doNothing().when(acknowledgment).acknowledge();

    UUID id = UUID.randomUUID();
    JobCommand jobCommand = createStartEHoldingsJobRequest(id);
    Thread mainThread = Thread.currentThread();

    doAnswer(invocationOnMock -> {
      Thread childThread = Thread.currentThread();
      Assertions.assertNotEquals(mainThread.getName(), childThread.getName());
      return null;
    }).when(exportJobManagerSync).launchJob(any());

    jobCommandsReceiverService.receiveStartJobCommand(jobCommand, acknowledgment);

    verify(exportJobManagerSync, timeout(1_000)).launchJob(any());
  }

  @Test
  @DisplayName("Delete files by kafka request")
  void deleteFilesTest() {
    doNothing().when(acknowledgment).acknowledge();

    UUID id = UUID.randomUUID();
    JobCommand jobCommand = createDeleteJobRequest(id);

    jobCommandsReceiverService.receiveStartJobCommand(jobCommand, acknowledgment);

    verify(acknowledgment, timeout(1_000)).acknowledge();
  }

  private JobCommand createStartCirculationLogJobRequest(UUID id) {
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

  private JobCommand createStartEHoldingsJobRequest(UUID id) {
    JobCommand jobCommand = new JobCommand();
    jobCommand.setType(JobCommand.Type.START);
    jobCommand.setId(id);
    jobCommand.setName(ExportType.E_HOLDINGS.toString());
    jobCommand.setDescription("Start job test desc");
    jobCommand.setExportType(ExportType.E_HOLDINGS);

    EHoldingsExportConfig eHoldingsExportConfig = new EHoldingsExportConfig();
    eHoldingsExportConfig.setRecordId(UUID.randomUUID().toString());
    eHoldingsExportConfig.setRecordType(EHoldingsExportConfig.RecordTypeEnum.RESOURCE);
    eHoldingsExportConfig.setPackageFields(Collections.emptyList());
    eHoldingsExportConfig.setTitleSearchFilters("");
    eHoldingsExportConfig.setTitleFields(Collections.emptyList());
    Map<String, JobParameter> params = new HashMap<>();
    params.put("eHoldingsExportConfig", new JobParameter(asJsonString(eHoldingsExportConfig)));
    jobCommand.setJobParameters(new JobParameters(params));
    return jobCommand;
  }

  private JobCommand createDeleteJobRequest(UUID id) {
    JobCommand jobCommand = new JobCommand();
    jobCommand.setType(JobCommand.Type.DELETE);
    jobCommand.setId(id);
    jobCommand.setJobParameters(
        new JobParameters(Collections.singletonMap(JobParameterNames.OUTPUT_FILES_IN_STORAGE, new JobParameter("https://x-host.com/560b33d8-7220-4c97-bfd1-dbc5b9c49537_duplicate.csv"))));
    return jobCommand;
  }

}
