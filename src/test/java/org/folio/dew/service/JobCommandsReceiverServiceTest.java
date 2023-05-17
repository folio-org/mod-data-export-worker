package org.folio.dew.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;
import org.folio.de.entity.JobCommand;
import org.folio.de.entity.JobCommandType;
import org.folio.dew.BaseBatchTest;
import org.folio.dew.batch.acquisitions.edifact.services.FTPStorageService;
import org.folio.dew.domain.dto.EHoldingsExportConfig;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.JobParameterNames;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.dew.domain.dto.authority.control.AuthorityControlExportConfig;
import org.folio.dew.repository.JobCommandRepository;
import org.folio.dew.repository.RemoteFilesStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.boot.test.mock.mockito.MockBean;

class JobCommandsReceiverServiceTest extends BaseBatchTest {

  @MockBean
  private JobCommandRepository jobCommandRepository;
  @MockBean
  RemoteFilesStorage remoteFilesStorage;
  @MockBean
  FTPStorageService ftpStorageService;

  @Test
  @DisplayName("Start CirculationLog job by kafka request")
  void startCirculationLogJobTest() throws JobExecutionException {

    UUID id = UUID.randomUUID();
    JobCommand jobCommand = createStartCirculationLogJobRequest(id);

    jobCommandsReceiverService.receiveStartJobCommand(jobCommand, okapiHeaders);

    verify(exportJobManagerSync, times(1)).launchJob(any());
  }

  @Test
  @DisplayName("Resend job by kafka request")
  void startResendTest() throws Exception {
    String testString = "Test string";

    doReturn(testString.getBytes()).when(remoteFilesStorage).readAllBytes(anyString());
    doNothing().when(ftpStorageService).uploadToFtp(any(), any(), anyString());

    UUID id = UUID.randomUUID();
    JobCommand jobCommand = createStartResendRequest(id);
    jobCommandsReceiverService.receiveStartJobCommand(jobCommand, okapiHeaders);

    verify(exportJobManagerSync, never()).launchJob(any());
  }

  @Test
  @DisplayName("Resend job should failed")
  void failedResendTest() throws Exception {
    String testString = "Test string";

    doReturn(testString.getBytes()).when(remoteFilesStorage).readAllBytes(anyString());
    doThrow(new Exception("Something went wrong")).when(ftpStorageService).uploadToFtp(any(), any(), anyString());

    UUID id = UUID.randomUUID();
    JobCommand jobCommand = createStartResendRequest(id);
    jobCommandsReceiverService.receiveStartJobCommand(jobCommand, okapiHeaders);

    verify(exportJobManagerSync, never()).launchJob(any());
  }

  @Test
  @DisplayName("Resend job should failed JobId is null")
  void failedResendTestJobIdIsNull() throws Exception {

    JobCommand jobCommand = createStartResendRequest(null);
    jobCommandsReceiverService.receiveStartJobCommand(jobCommand, okapiHeaders);

    verify(ftpStorageService, never()).uploadToFtp(any(), any(), anyString());
    verify(exportJobManagerSync, never()).launchJob(any());
  }

  @Test
  @DisplayName("Start EHoldings job by kafka request")
  void startEHoldingsJobTest() throws JobExecutionException {
    UUID id = UUID.randomUUID();
    JobCommand jobCommand = createStartEHoldingsJobRequest(id);

    jobCommandsReceiverService.receiveStartJobCommand(jobCommand, okapiHeaders);

    verify(exportJobManagerSync, times(1)).launchJob(any());
  }

  @Test
  @DisplayName("Start Authority Control job by kafka request")
  void startAuthorityControlForAuthorityJobTest() throws JobExecutionException {
    UUID id = UUID.randomUUID();
    JobCommand jobCommand = createStartAuthorityControlAuthorityJobRequest(id);

    jobCommandsReceiverService.receiveStartJobCommand(jobCommand, okapiHeaders);

    verify(exportJobManagerSync, times(1)).launchJob(any());
  }

  @Test
  @DisplayName("Start Authority Control job by kafka request")
  void startAuthorityControlForInstanceJobTest() throws JobExecutionException {
    UUID id = UUID.randomUUID();
    JobCommand jobCommand = createStartAuthorityControlInstanceJobRequest(id);

    jobCommandsReceiverService.receiveStartJobCommand(jobCommand, okapiHeaders);

    verify(exportJobManagerSync, times(1)).launchJob(any());
  }

  @Test
  @DisplayName("Delete files by kafka request")
  void deleteFilesTest() {
    UUID id = UUID.randomUUID();
    JobCommand jobCommand = createDeleteJobRequest(id);

    jobCommandsReceiverService.receiveStartJobCommand(jobCommand, okapiHeaders);
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

    var paramBuilder = new JobParametersBuilder();
    paramBuilder.addString("query", "");
    jobCommand.setJobParameters(paramBuilder.toJobParameters());
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

    var paramsBuilder = new JobParametersBuilder();
    paramsBuilder.addString("FILE_NAME", "TestFile.csv");
    paramsBuilder.addString("EDIFACT_ORDERS_EXPORT", asJsonString(config));

    jobCommand.setJobParameters(paramsBuilder.toJobParameters());
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
    var paramsBuilder = new JobParametersBuilder();
    paramsBuilder.addString("eHoldingsExportConfig", asJsonString(eHoldingsExportConfig));
    jobCommand.setJobParameters(paramsBuilder.toJobParameters());
    return jobCommand;
  }

  private JobCommand createStartAuthorityControlAuthorityJobRequest(UUID id) {
    JobCommand jobCommand = new JobCommand();
    jobCommand.setType(JobCommandType.START);
    jobCommand.setId(id);
    jobCommand.setName(ExportType.AUTH_HEADINGS_UPDATES.toString());
    jobCommand.setDescription("Start job test desc");
    jobCommand.setExportType(ExportType.AUTH_HEADINGS_UPDATES);

    AuthorityControlExportConfig authorityControlExportConfig = new AuthorityControlExportConfig();
    authorityControlExportConfig.fromDate(LocalDate.now());
    authorityControlExportConfig.toDate(LocalDate.now());
    var paramsBuilder = new JobParametersBuilder();
    paramsBuilder.addString("authorityControlExportConfig", asJsonString(authorityControlExportConfig));
    jobCommand.setJobParameters(paramsBuilder.toJobParameters());
    return jobCommand;
  }

  private JobCommand createStartAuthorityControlInstanceJobRequest(UUID id) {
    JobCommand jobCommand = new JobCommand();
    jobCommand.setType(JobCommandType.START);
    jobCommand.setId(id);
    jobCommand.setName(ExportType.FAILED_LINKED_BIB_UPDATES.toString());
    jobCommand.setDescription("Start job test desc");
    jobCommand.setExportType(ExportType.AUTH_HEADINGS_UPDATES);

    AuthorityControlExportConfig authorityControlExportConfig = new AuthorityControlExportConfig();
    authorityControlExportConfig.fromDate(LocalDate.now());
    authorityControlExportConfig.toDate(LocalDate.now());
    var paramsBuilder = new JobParametersBuilder();
    paramsBuilder.addString("authorityControlExportConfig", asJsonString(authorityControlExportConfig));
    jobCommand.setJobParameters(paramsBuilder.toJobParameters());
    return jobCommand;
  }

  private JobCommand createDeleteJobRequest(UUID id) {
    JobCommand jobCommand = new JobCommand();
    jobCommand.setType(JobCommandType.DELETE);
    jobCommand.setId(id);
    jobCommand.setJobParameters(
        new JobParameters(Collections.singletonMap(JobParameterNames.OUTPUT_FILES_IN_STORAGE, new JobParameter("https://x-host.com/560b33d8-7220-4c97-bfd1-dbc5b9c49537_duplicate.csv", String.class))));
    return jobCommand;
  }

}
