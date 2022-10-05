package org.folio.dew.service;

import static org.folio.dew.domain.dto.JobParameterNames.JOB_ID;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.folio.de.entity.JobCommand;
import org.folio.de.entity.JobCommandType;
import org.folio.dew.BaseBatchTest;
import org.folio.dew.domain.dto.EHoldingsExportConfig;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.JobParameterNames;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.dew.repository.JobCommandRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.support.Acknowledgment;

class JobCommandsReceiverServiceTest extends BaseBatchTest {

  @MockBean
  private JobCommandRepository jobCommandRepository;

  @Test
  @DisplayName("Start CirculationLog job by kafka request")
  void startCirculationLogJobTest() throws JobExecutionException {
    doNothing().when(acknowledgment).acknowledge();

    UUID id = UUID.randomUUID();
    JobCommand jobCommand = createStartCirculationLogJobRequest(id);

    jobCommandsReceiverService.receiveStartJobCommand(jobCommand, acknowledgment);

    verify(exportJobManagerSync, times(1)).launchJob(any());

    final Acknowledgment savedAcknowledgment = repository.getAcknowledgement(id.toString());

    assertNotNull(savedAcknowledgment);
  }

  @Test
  @DisplayName("Resend job by kafka request")
  void startResendTest() throws JobExecutionException {
    doNothing().when(acknowledgment).acknowledge();

    UUID id = UUID.randomUUID();
    JobCommand jobCommand = createStartResendRequest(id);

    jobCommandsReceiverService.receiveStartJobCommand(jobCommand, acknowledgment);

    verify(exportJobManagerSync, never()).launchJob(any());
  }

  @Test
  @DisplayName("Start EHoldings job by kafka request")
  void startEHoldingsJobTest() throws JobExecutionException {
    doNothing().when(acknowledgment).acknowledge();

    UUID id = UUID.randomUUID();
    JobCommand jobCommand = createStartEHoldingsJobRequest(id);

    jobCommandsReceiverService.receiveStartJobCommand(jobCommand, acknowledgment);

    verify(exportJobManagerSync, times(1)).launchJob(any());

    final Acknowledgment savedAcknowledgment = repository.getAcknowledgement(id.toString());

    assertNotNull(savedAcknowledgment);
  }

  @Test
  @DisplayName("Delete files by kafka request")
  void deleteFilesTest() {
    doNothing().when(acknowledgment).acknowledge();

    UUID id = UUID.randomUUID();
    JobCommand jobCommand = createDeleteJobRequest(id);

    jobCommandsReceiverService.receiveStartJobCommand(jobCommand, acknowledgment);

    verify(acknowledgment, times(1)).acknowledge();
  }

  @Test
  void addBulkEditJobCommandIfJobCommandDoesNotExistTest() {
    var jobId = UUID.randomUUID();
    var jobCommand = new JobCommand();
    jobCommand.setId(jobId);

    when(jobCommandRepository.existsById(jobId)).thenReturn(false);

    jobCommandsReceiverService.addBulkEditJobCommand(jobCommand);

    verify(jobCommandRepository, times(1)).save(any());
  }

  @Test
  void addBulkEditJobCommandIfJobCommandExistTest() {
    var jobId = UUID.randomUUID();
    var jobCommand = new JobCommand();
    jobCommand.setId(jobId);

    when(jobCommandRepository.existsById(jobId)).thenReturn(true);

    jobCommandsReceiverService.addBulkEditJobCommand(jobCommand);

    verify(jobCommandRepository, times(0)).save(any());
  }

  @Test
  void getBulkEditJobCommandByIdTest() {
    var jobId = UUID.randomUUID();
    var jobCommand = new JobCommand();
    jobCommand.setId(jobId);

    jobCommandsReceiverService.getBulkEditJobCommandById(jobId.toString());

    verify(jobCommandRepository, times(1)).findById(jobId);
  }

  @Test
  void updateJobCommandTest() {
    var jobId = UUID.randomUUID();
    var jobCommand = new JobCommand();
    jobCommand.setId(jobId);

    jobCommandsReceiverService.updateJobCommand(jobCommand);

    verify(jobCommandRepository, times(1)).save(any());
  }

  private JobCommand createStartCirculationLogJobRequest(UUID id) {
    JobCommand jobCommand = new JobCommand();
    jobCommand.setType(JobCommandType.START);
    jobCommand.setId(id);
    jobCommand.setName(ExportType.CIRCULATION_LOG.toString());
    jobCommand.setDescription("Start job test desc");
    jobCommand.setExportType(ExportType.CIRCULATION_LOG);

    Map<String, JobParameter> params = new HashMap<>();
    params.put("query", new JobParameter(""));
    jobCommand.setJobParameters(new JobParameters(params));
    return jobCommand;
  }

  private JobCommand createStartResendRequest(UUID id) {
    JobCommand jobCommand = new JobCommand();
    jobCommand.setType(JobCommandType.RESEND);
    jobCommand.setId(id);
    jobCommand.setName(ExportType.EDIFACT_ORDERS_EXPORT.toString());
    jobCommand.setDescription("Resent job test desc");
    jobCommand.setExportType(ExportType.EDIFACT_ORDERS_EXPORT);

    VendorEdiOrdersExportConfig config = new VendorEdiOrdersExportConfig();
    config.setVendorId(UUID.randomUUID());

    Map<String, JobParameter> params = new HashMap<>();
    params.put(JOB_ID, new JobParameter(id.toString()));
    params.put("FILE_NAME", new JobParameter("TestFile.csv"));
    params.put("EDIFACT_ORDERS_EXPORT", new JobParameter(asJsonString(config)));

    jobCommand.setJobParameters(new JobParameters(params));
    return jobCommand;
  }

  private JobCommand createStartEHoldingsJobRequest(UUID id) {
    JobCommand jobCommand = new JobCommand();
    jobCommand.setType(JobCommandType.START);
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
    jobCommand.setType(JobCommandType.DELETE);
    jobCommand.setId(id);
    jobCommand.setJobParameters(
        new JobParameters(Collections.singletonMap(JobParameterNames.OUTPUT_FILES_IN_STORAGE, new JobParameter("https://x-host.com/560b33d8-7220-4c97-bfd1-dbc5b9c49537_duplicate.csv"))));
    return jobCommand;
  }

}
