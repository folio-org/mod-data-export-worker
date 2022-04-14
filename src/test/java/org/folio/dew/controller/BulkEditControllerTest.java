package org.folio.dew.controller;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.dew.domain.dto.EntityType.ITEM;
import static org.folio.dew.domain.dto.EntityType.USER;
import static org.folio.dew.domain.dto.ExportType.BULK_EDIT_IDENTIFIERS;
import static org.folio.dew.domain.dto.IdentifierType.BARCODE;
import static org.folio.dew.domain.dto.JobParameterNames.TEMP_OUTPUT_FILE_PATH;
import static org.folio.dew.domain.dto.JobParameterNames.UPDATED_FILE_NAME;
import static org.folio.dew.utils.BulkEditProcessorHelper.resolveIdentifier;
import static org.folio.dew.utils.Constants.CSV_EXTENSION;
import static org.folio.dew.utils.Constants.FILE_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.batch.test.AssertFile.assertFileEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.commons.io.FilenameUtils;
import org.folio.de.entity.JobCommand;
import org.folio.dew.BaseBatchTest;
import org.folio.dew.client.InventoryClient;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.*;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.repository.MinIOObjectStorageRepository;
import org.folio.dew.service.BulkEditProcessingErrorsService;
import org.folio.dew.service.BulkEditRollBackService;
import org.folio.dew.service.JobCommandsReceiverService;
import org.folio.tenant.domain.dto.Errors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.integration.launch.JobLaunchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class BulkEditControllerTest extends BaseBatchTest {
  private static final String UPLOAD_URL_TEMPLATE = "/bulk-edit/%s/upload";
  private static final String START_URL_TEMPLATE = "/bulk-edit/%s/start";
  private static final String PREVIEW_USERS_URL_TEMPLATE = "/bulk-edit/%s/preview/users";
  private static final String PREVIEW_ITEMS_URL_TEMPLATE = "/bulk-edit/%s/preview/items";
  private static final String ERRORS_URL_TEMPLATE = "/bulk-edit/%s/errors";
  private static final String ITEMS_CONTENT_UPDATE_UPLOAD_URL_TEMPLATE = "/bulk-edit/%s/items-content-update/upload";
  private static final String ITEMS_CONTENT_PREVIEW_DOWNLOAD_URL_TEMPLATE = "/bulk-edit/%s/preview/updated-items/download";
  private static final String ITEMS_FOR_LOCATION_UPDATE = "src/test/resources/upload/bulk_edit_items_for_location_update.csv";
  public static final String LIMIT = "limit";

  @MockBean
  private BulkEditRollBackService bulkEditRollBackService;

  @MockBean
  private UserClient userClient;

  @MockBean
  private InventoryClient inventoryClient;

  @Autowired
  private BulkEditProcessingErrorsService bulkEditProcessingErrorsService;

  @Autowired
  private MinIOObjectStorageRepository repository;

  @Autowired
  private JobCommandsReceiverService jobCommandsReceiverService;

  @Test
  void shouldReturnErrorsPreview() throws Exception {

    var jobId = UUID.randomUUID();
    jobCommandsReceiverService.addBulkEditJobCommand(createBulkEditJobRequest(jobId, BULK_EDIT_IDENTIFIERS, USER, BARCODE));

    int numOfErrorLines = 3;
    int errorsPreviewLimit = 2;
    var reasonForError = new BulkEditException("Record not found");
    var fileName = "barcodes.csv";
    for (int i = 0; i < numOfErrorLines; i++) {
      bulkEditProcessingErrorsService.saveErrorInCSV(jobId.toString(), String.valueOf(i), reasonForError, fileName);
    }
    var headers = defaultHeaders();

    var response = mockMvc.perform(get(format(ERRORS_URL_TEMPLATE, jobId))
        .headers(headers)
        .queryParam(LIMIT, String.valueOf(errorsPreviewLimit)))
      .andExpect(status().isOk());

    var errors = objectMapper.readValue(response.andReturn().getResponse().getContentAsString(), Errors.class);

    assertThat(errors.getErrors(), hasSize(errorsPreviewLimit));
    assertThat(errors.getTotalRecords(), is(errorsPreviewLimit));

    bulkEditProcessingErrorsService.removeTemporaryErrorStorage(jobId.toString());
  }

  @Test
  void shouldReturnEmptyErrorsForErrorsPreview() throws Exception {

    var jobId = UUID.randomUUID();
    jobCommandsReceiverService.addBulkEditJobCommand(createBulkEditJobRequest(jobId, BULK_EDIT_IDENTIFIERS, USER, BARCODE));

    var expectedErrorMsg = format("errors file for job id %s", jobId);

    var headers = defaultHeaders();

    var response = mockMvc.perform(get(format(ERRORS_URL_TEMPLATE, jobId))
        .headers(headers)
        .queryParam(LIMIT, String.valueOf(2)))
      .andExpect(status().isOk());

    var errors = objectMapper.readValue(response.andReturn().getResponse().getContentAsString(), Errors.class);

    assertThat(errors.getErrors(), empty());
    assertThat(errors.getTotalRecords(), is(0));
  }

  @Test
  void shouldReturnErrorsFileNotFoundErrorForErrorsPreview() throws Exception {

    var jobId = UUID.randomUUID();
    var expectedJson = String.format("{\"errors\":[{\"message\":\"JobCommand with id %s doesn't exist.\",\"type\":\"-1\",\"code\":\"Not found\",\"parameters\":null}],\"total_records\":1}", jobId);

    var headers = defaultHeaders();

    mockMvc.perform(get(format(ERRORS_URL_TEMPLATE, jobId))
        .headers(headers)
        .queryParam(LIMIT, String.valueOf(2)))
      .andExpect(status().isNotFound())
      .andExpect(content().json(expectedJson));
  }

  @SneakyThrows
  @ParameterizedTest
  @CsvSource({"BULK_EDIT_IDENTIFIERS,barcode==(123 OR 456 OR 789)",
    "BULK_EDIT_QUERY,(patronGroup==\"3684a786-6671-4268-8ed0-9db82ebca60b\") sortby personal.lastName"})
  void shouldReturnCompleteUserPreview(String exportType, String query) {

    when(userClient.getUserByQuery(query, 3)).thenReturn(buildUserCollection());

    var jobId = UUID.randomUUID();
    jobCommandsReceiverService.addBulkEditJobCommand(createBulkEditJobRequest(jobId, ExportType.fromValue(exportType), USER, BARCODE));

    var headers = defaultHeaders();

    var response = mockMvc.perform(get(format(PREVIEW_USERS_URL_TEMPLATE, jobId))
        .headers(headers)
        .queryParam(LIMIT, String.valueOf(3)))
      .andExpect(status().isOk());

    var users = objectMapper.readValue(response.andReturn().getResponse().getContentAsString(), UserCollection.class);
    assertThat(users.getTotalRecords(), equalTo(3));
    assertThat(users.getUsers(), hasSize(3));
  }

  @ParameterizedTest
  @EnumSource(IdentifierType.class)
  @SneakyThrows
  void shouldReturnCompleteItemPreview(IdentifierType identifierType) {

    when(inventoryClient.getItemByQuery(String.format("%s==(123 OR 456 OR 789)", resolveIdentifier(identifierType.getValue())), 3)).thenReturn(buildItemCollection());

    var jobId = UUID.randomUUID();
    jobCommandsReceiverService.addBulkEditJobCommand(createBulkEditJobRequest(jobId, BULK_EDIT_IDENTIFIERS, ITEM, identifierType));

    var headers = defaultHeaders();

    var response = mockMvc.perform(get(format(PREVIEW_ITEMS_URL_TEMPLATE, jobId))
        .headers(headers)
        .queryParam(LIMIT, String.valueOf(3)))
      .andExpect(status().isOk());

    var items = objectMapper.readValue(response.andReturn().getResponse().getContentAsString(), ItemCollection.class);
    assertThat(items.getTotalRecords(), equalTo(3));
    assertThat(items.getItems(), hasSize(3));
  }

  @SneakyThrows
  @ParameterizedTest
  @CsvSource({"BULK_EDIT_IDENTIFIERS,barcode==(123 OR 456)",
    "BULK_EDIT_UPDATE,barcode==(123 OR 456)",
    "BULK_EDIT_QUERY,(patronGroup==\"3684a786-6671-4268-8ed0-9db82ebca60b\") sortby personal.lastName"})
  void shouldReturnCompleteUserPreviewWithLimitControl(String exportType, String query) {

    when(userClient.getUserByQuery(query, 2)).thenReturn(buildUserCollection());

    var jobId = UUID.randomUUID();
    jobCommandsReceiverService.addBulkEditJobCommand(createBulkEditJobRequest(jobId, ExportType.fromValue(exportType), USER, BARCODE));

    var headers = defaultHeaders();

    var response = mockMvc.perform(get(format(PREVIEW_USERS_URL_TEMPLATE, jobId))
        .headers(headers)
        .queryParam(LIMIT, String.valueOf(2)))
      .andExpect(status().isOk());

    ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Long> limitCaptor = ArgumentCaptor.forClass(Long.class);
    verify(userClient).getUserByQuery(queryCaptor.capture(), limitCaptor.capture());
    assertThat(query, equalTo(queryCaptor.getValue()));
    assertThat(2L, equalTo(limitCaptor.getValue()));
  }

  @SneakyThrows
  @ParameterizedTest
  @CsvSource({"BULK_EDIT_IDENTIFIERS,barcode==(123 OR 456)",
    "BULK_EDIT_UPDATE,barcode==(123 OR 456)"})
  void shouldReturnCompleteItemsPreviewWithLimitControl(String exportType, String query) {

    when(inventoryClient.getItemByQuery(query, 2)).thenReturn(buildItemCollection());

    var jobId = UUID.randomUUID();
    jobCommandsReceiverService.addBulkEditJobCommand(createBulkEditJobRequest(jobId, ExportType.fromValue(exportType), ITEM, BARCODE));

    var headers = defaultHeaders();

    var response = mockMvc.perform(get(format(PREVIEW_ITEMS_URL_TEMPLATE, jobId))
        .headers(headers)
        .queryParam(LIMIT, String.valueOf(2)))
      .andExpect(status().isOk());

    ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Long> limitCaptor = ArgumentCaptor.forClass(Long.class);
    verify(inventoryClient).getItemByQuery(queryCaptor.capture(), limitCaptor.capture());
    assertThat(query, equalTo(queryCaptor.getValue()));
    assertThat(2L, equalTo(limitCaptor.getValue()));
  }

  @ParameterizedTest
  @EnumSource(IdentifierType.class)
  @SneakyThrows
  void shouldReturnCompleteItemPreviewWithDifferentIdentifiers(IdentifierType identifierType) {

    var query = String.format("%s==(123 OR 456)", resolveIdentifier(identifierType.getValue()));
    when(inventoryClient.getItemByQuery(query, 2)).thenReturn(buildItemCollection());

    var jobId = UUID.randomUUID();
    jobCommandsReceiverService.addBulkEditJobCommand(createBulkEditJobRequest(jobId, BULK_EDIT_IDENTIFIERS, ITEM, identifierType));

    var headers = defaultHeaders();

    var response = mockMvc.perform(get(format(PREVIEW_ITEMS_URL_TEMPLATE, jobId))
        .headers(headers)
        .queryParam(LIMIT, String.valueOf(2)))
      .andExpect(status().isOk());

    ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Long> limitCaptor = ArgumentCaptor.forClass(Long.class);
    verify(inventoryClient).getItemByQuery(queryCaptor.capture(), limitCaptor.capture());
    assertThat(query, equalTo(queryCaptor.getValue()));
    assertThat(2L, equalTo(limitCaptor.getValue()));
  }

  @SneakyThrows
  @Test
  void shouldReturnErrorForInvalidExportType() {

    var jobId = UUID.randomUUID();
    jobCommandsReceiverService.addBulkEditJobCommand(createBulkEditJobRequest(jobId, ExportType.ORDERS_EXPORT, USER, BARCODE));

    var headers = defaultHeaders();

    mockMvc.perform(get(format(PREVIEW_USERS_URL_TEMPLATE, jobId))
        .headers(headers)
        .queryParam(LIMIT, String.valueOf(2)))
      .andExpect(status().isBadRequest());
  }

  @SneakyThrows
  @Test
  void shouldReturnErroJobNotFound() {

    var headers = defaultHeaders();

    mockMvc.perform(get(format(PREVIEW_USERS_URL_TEMPLATE, UUID.randomUUID()))
        .headers(headers)
        .queryParam(LIMIT, String.valueOf(2)))
      .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Launch job on upload file with identifiers successfully")
  @SneakyThrows
  void shouldLaunchJobAndReturnNumberOfRecordsOnIdentifiersFileUpload() {
    var jobId = UUID.randomUUID();
    jobCommandsReceiverService.addBulkEditJobCommand(createBulkEditJobRequest(jobId, BULK_EDIT_IDENTIFIERS, USER, BARCODE));

    var headers = defaultHeaders();

    var bytes = new FileInputStream("src/test/resources/upload/barcodes.csv").readAllBytes();
    var file = new MockMultipartFile("file", "barcodes.csv", MediaType.TEXT_PLAIN_VALUE, bytes);

    var result = mockMvc.perform(multipart(format(UPLOAD_URL_TEMPLATE, jobId))
      .file(file)
      .headers(headers))
      .andExpect(status().isOk())
      .andReturn();

    assertThat(result.getResponse().getContentAsString(), equalTo("3"));

    verify(exportJobManager, times(1)).launchJob(any());
  }

  @Test
  @DisplayName("Skip headers while counting records for update")
  @SneakyThrows
  void shouldSkipHeadersWhileCountingRecordsForUpdate() {
    var jobId = UUID.randomUUID();
    jobCommandsReceiverService.addBulkEditJobCommand(createBulkEditJobRequest(jobId, ExportType.BULK_EDIT_UPDATE, USER, BARCODE));

    var headers = defaultHeaders();

    var bytes = new FileInputStream("src/test/resources/upload/bulk_edit_user_record.csv").readAllBytes();
    var file = new MockMultipartFile("file", "bulk_edit_user_record.csv", MediaType.TEXT_PLAIN_VALUE, bytes);

    var result = mockMvc.perform(multipart(format(UPLOAD_URL_TEMPLATE, jobId))
        .file(file)
        .headers(headers))
      .andExpect(status().isOk())
      .andReturn();

    assertThat(result.getResponse().getContentAsString(), equalTo("1"));
  }

  @Test
  @DisplayName("Upload empty file - BAD REQUEST")
  @SneakyThrows
  void shouldReturnBadRequestWhenIdentifiersFileIsEmpty() {
    var jobId = UUID.randomUUID();
    jobCommandsReceiverService.addBulkEditJobCommand(createBulkEditJobRequest(jobId, BULK_EDIT_IDENTIFIERS, USER, BARCODE));

    var headers = defaultHeaders();

    var file = new MockMultipartFile("file", "barcodes.csv", MediaType.TEXT_PLAIN_VALUE, new byte[]{});

    mockMvc.perform(multipart(format(UPLOAD_URL_TEMPLATE, jobId))
        .file(file)
        .headers(headers))
      .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Job doesn't exist - NOT FOUND")
  @SneakyThrows
  void shouldReturnNotFoundIfJobDoesNotExist() {
    var jobId = UUID.randomUUID();

    var headers = defaultHeaders();

    var bytes = new FileInputStream("src/test/resources/upload/barcodes.csv").readAllBytes();
    var file = new MockMultipartFile("file", "barcodes.csv", MediaType.TEXT_PLAIN_VALUE, bytes);

    mockMvc.perform(multipart(format(UPLOAD_URL_TEMPLATE, jobId))
        .file(file)
        .headers(headers))
      .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Start update job test")
  @SneakyThrows
  void startUpdateJobTest() {
    var jobId = UUID.fromString("edd30136-9a7b-4226-9e82-83024dbeac4a");
    var jobCommand = new JobCommand();
    jobCommand.setExportType(ExportType.BULK_EDIT_UPDATE);
    jobCommand.setEntityType(USER);
    jobCommand.setJobParameters(new JobParameters(new HashMap<String, JobParameter>()));
    var executionId = 0l;
    var jobExecution = new JobExecution(executionId);

    var headers = defaultHeaders();

    when(jobCommandsReceiverService.getBulkEditJobCommandById(jobId.toString())).thenReturn(Optional.of(jobCommand));
    when(exportJobManager.launchJob(isA(JobLaunchRequest.class))).thenReturn(jobExecution);

    mockMvc.perform(multipart(format(START_URL_TEMPLATE, jobId))
      .headers(headers))
      .andExpect(status().isOk());

    verify(jobCommandsReceiverService, times(1)).getBulkEditJobCommandById(jobId.toString());
    verify(exportJobManager, times(1)).launchJob(isA(JobLaunchRequest.class));
    verify(bulkEditRollBackService, times(1)).putExecutionInfoPerJob(executionId, jobId);
  }

  @Test
  @DisplayName("Job doesn't exist - NOT FOUND")
  @SneakyThrows
  void startUpdateJobReturnNotFoundTest() {
    var jobId = UUID.fromString("edd30136-9a7b-4226-9e82-83024dbeac4a");
    var headers = defaultHeaders();

    when(jobCommandsReceiverService.getBulkEditJobCommandById(jobId.toString())).thenReturn(Optional.empty());

    mockMvc.perform(multipart(format(START_URL_TEMPLATE, jobId))
      .headers(headers))
      .andExpect(status().isNotFound());

    verify(jobCommandsReceiverService, times(1)).getBulkEditJobCommandById(jobId.toString());
  }

  @Test
  @DisplayName("Start update job - INTERNAL SERVER ERROR")
  @SneakyThrows
  void startUpdateJobReturnInternalServerErrorTest() {
    var jobId = UUID.fromString("edd30136-9a7b-4226-9e82-83024dbeac4a");
    var jobCommand = new JobCommand();
    jobCommand.setExportType(ExportType.BULK_EDIT_UPDATE);
    jobCommand.setEntityType(USER);
    jobCommand.setJobParameters(new JobParameters(new HashMap<String, JobParameter>()));

    var headers = defaultHeaders();

    when(jobCommandsReceiverService.getBulkEditJobCommandById(jobId.toString())).thenReturn(Optional.of(jobCommand));
    when(exportJobManager.launchJob(isA(JobLaunchRequest.class))).thenThrow(new JobExecutionException("Execution exception"));

    mockMvc.perform(multipart(format(START_URL_TEMPLATE, jobId))
      .headers(headers))
      .andExpect(status().isInternalServerError());
  }

  @ParameterizedTest
  @EnumSource(ItemsContentUpdateTestData.class)
  @DisplayName("Post content updates - successful")
  @SneakyThrows
  void shouldUpdateEffectiveLocationOnChangeLocationContentUpdate(ItemsContentUpdateTestData testData) {
    repository.uploadObject(FilenameUtils.getName(ITEMS_FOR_LOCATION_UPDATE), ITEMS_FOR_LOCATION_UPDATE, null, "text/plain", false);
    var jobId = UUID.randomUUID();
    var jobCommand = new JobCommand();
    jobCommand.setId(jobId);
    jobCommand.setExportType(ExportType.BULK_EDIT_UPDATE);
    jobCommand.setEntityType(ITEM);
    jobCommand.setJobParameters(new JobParametersBuilder()
      .addString(TEMP_OUTPUT_FILE_PATH, "test/path/" + ITEMS_FOR_LOCATION_UPDATE.replace(CSV_EXTENSION, EMPTY))
      .toJobParameters());

    jobCommandsReceiverService.addBulkEditJobCommand(jobCommand);

    var updates = objectMapper.writeValueAsString(new ContentUpdateCollection()
      .entityType(ITEM)
      .contentUpdates(Collections.singletonList(new ContentUpdate()
        .option(testData.getOption())
        .action(testData.getAction())
        .value(testData.getValue())))
      .totalRecords(1));

    var response = mockMvc.perform(post(format(ITEMS_CONTENT_UPDATE_UPLOAD_URL_TEMPLATE, jobId))
      .headers(defaultHeaders())
      .content(updates))
      .andExpect(status().isOk())
      .andReturn();

    var updatedJobCommand = jobCommandsReceiverService.getBulkEditJobCommandById(jobId.toString());
    assertFalse(updatedJobCommand.isEmpty());
    assertNotNull(updatedJobCommand.get().getJobParameters().getString(UPDATED_FILE_NAME));

    var expectedCsv = new FileSystemResource(testData.getExpectedCsvPath());
    var actualCsv = new FileSystemResource(updatedJobCommand.get().getJobParameters().getString(UPDATED_FILE_NAME));
    assertFileEquals(expectedCsv, actualCsv);

    var actualItems = objectMapper.readValue(response.getResponse().getContentAsString(), ItemCollection.class);
    var expectedItems = objectMapper.readValue(new FileSystemResource(testData.getExpectedJsonPath()).getInputStream(), ItemCollection.class);
    verifyLocationUpdate(expectedItems, actualItems);
  }

  @ParameterizedTest
  @EnumSource(ItemsContentUpdateTestData.class)
  @DisplayName("Download preview - successful")
  @SneakyThrows
  void shouldDownloadPreviewAfterContentUpdate(ItemsContentUpdateTestData testData) {
    repository.uploadObject(FilenameUtils.getName(ITEMS_FOR_LOCATION_UPDATE), ITEMS_FOR_LOCATION_UPDATE, null, "text/plain", false);
    var jobId = UUID.randomUUID();
    var jobCommand = new JobCommand();
    jobCommand.setId(jobId);
    jobCommand.setExportType(ExportType.BULK_EDIT_UPDATE);
    jobCommand.setEntityType(ITEM);
    jobCommand.setJobParameters(new JobParametersBuilder()
      .addString(TEMP_OUTPUT_FILE_PATH, "test/path/" + ITEMS_FOR_LOCATION_UPDATE.replace(CSV_EXTENSION, EMPTY))
      .toJobParameters());

    jobCommandsReceiverService.addBulkEditJobCommand(jobCommand);

    var updates = objectMapper.writeValueAsString(new ContentUpdateCollection()
      .entityType(ITEM)
      .contentUpdates(Collections.singletonList(new ContentUpdate()
        .option(testData.getOption())
        .action(testData.getAction())
        .value(testData.getValue())))
      .totalRecords(1));

    var response = mockMvc.perform(post(format(ITEMS_CONTENT_UPDATE_UPLOAD_URL_TEMPLATE, jobId))
      .headers(defaultHeaders())
      .content(updates))
      .andExpect(status().isOk())
      .andReturn();

    response = mockMvc.perform(get(format(ITEMS_CONTENT_PREVIEW_DOWNLOAD_URL_TEMPLATE, jobId))
      .headers(defaultHeaders()))
      .andExpect(status().isOk())
      .andReturn();

    var expectedCsv = new FileSystemResource(testData.getExpectedCsvPath());
    var actualCsvByteArr = response.getResponse().getContentAsByteArray();
    Path actualDownloadedCsvTmp = Paths.get("actualDownloaded.csv");
    Files.write(actualDownloadedCsvTmp, actualCsvByteArr);
    var actualCsv = new FileSystemResource(actualDownloadedCsvTmp);

    assertFileEquals(expectedCsv, actualCsv);

    Files.delete(actualDownloadedCsvTmp);
  }

  @ParameterizedTest
  @EnumSource(ItemsContentUpdateTestData.class)
  @DisplayName("Download preview - Job not found by ID - NOT FOUND")
  @SneakyThrows
  void shouldReturn404causeJobNotFoundById(ItemsContentUpdateTestData testData) {
    repository.uploadObject(FilenameUtils.getName(ITEMS_FOR_LOCATION_UPDATE), ITEMS_FOR_LOCATION_UPDATE, null, "text/plain", false);
    var jobId = UUID.randomUUID();
    var jobCommand = new JobCommand();
    jobCommand.setId(jobId);
    jobCommand.setExportType(ExportType.BULK_EDIT_UPDATE);
    jobCommand.setEntityType(ITEM);
    jobCommand.setJobParameters(new JobParametersBuilder()
      .addString(TEMP_OUTPUT_FILE_PATH, "test/path/" + ITEMS_FOR_LOCATION_UPDATE.replace(CSV_EXTENSION, EMPTY))
      .toJobParameters());

    jobCommandsReceiverService.addBulkEditJobCommand(jobCommand);

    var updates = objectMapper.writeValueAsString(new ContentUpdateCollection()
      .entityType(ITEM)
      .contentUpdates(Collections.singletonList(new ContentUpdate()
        .option(testData.getOption())
        .action(testData.getAction())
        .value(testData.getValue())))
      .totalRecords(1));

    mockMvc.perform(post(format(ITEMS_CONTENT_UPDATE_UPLOAD_URL_TEMPLATE, jobId))
      .headers(defaultHeaders())
      .content(updates))
      .andExpect(status().isOk())
      .andReturn();

    UUID jobByThisIdCannotBeFound = UUID.randomUUID();

    mockMvc.perform(get(format(ITEMS_CONTENT_PREVIEW_DOWNLOAD_URL_TEMPLATE, jobByThisIdCannotBeFound))
      .headers(defaultHeaders()))
      .andExpect(status().isNotFound())
      .andReturn();
  }

  @Test
  @DisplayName("Post content updates with limit - successful")
  @SneakyThrows
  void shouldLimitItemsInResponseWhenLimitIsNotNull() {
    repository.uploadObject(FilenameUtils.getName(ITEMS_FOR_LOCATION_UPDATE), ITEMS_FOR_LOCATION_UPDATE, null, "text/plain", false);
    var jobId = UUID.randomUUID();
    var jobCommand = new JobCommand();
    jobCommand.setId(jobId);
    jobCommand.setExportType(ExportType.BULK_EDIT_UPDATE);
    jobCommand.setEntityType(ITEM);
    jobCommand.setJobParameters(new JobParametersBuilder()
      .addString(TEMP_OUTPUT_FILE_PATH, "test/path/" + ITEMS_FOR_LOCATION_UPDATE.replace(CSV_EXTENSION, EMPTY))
      .toJobParameters());
    when(jobCommandsReceiverService.getBulkEditJobCommandById(jobId.toString())).thenReturn(Optional.of(jobCommand));

    var updates = objectMapper.writeValueAsString(new ContentUpdateCollection()
      .entityType(ITEM)
      .contentUpdates(Collections.singletonList(new ContentUpdate()
        .option(ContentUpdate.OptionEnum.TEMPORARY_LOCATION)
        .action(ContentUpdate.ActionEnum.REPLACE_WITH)
        .value("Annex")))
      .totalRecords(1));

    var response = mockMvc.perform(post(format(ITEMS_CONTENT_UPDATE_UPLOAD_URL_TEMPLATE + "?limit=2", jobId))
        .headers(defaultHeaders())
        .content(updates))
      .andExpect(status().isOk())
      .andReturn();

    var actualItems = objectMapper.readValue(response.getResponse().getContentAsString(), ItemCollection.class);

    assertThat(actualItems.getItems(), hasSize(2));
  }

  @Test
  @DisplayName("Post non-supported entity type content update - BAD REQUEST")
  @SneakyThrows
  void shouldReturnBadRequestForNonSupportedEntityTypeContentUpdates() {
    var updates = new ObjectMapper().writeValueAsString(new ContentUpdateCollection()
      .entityType(USER)
      .contentUpdates(Collections.singletonList(new ContentUpdate()
        .option(ContentUpdate.OptionEnum.TEMPORARY_LOCATION)
        .action(ContentUpdate.ActionEnum.REPLACE_WITH)))
      .totalRecords(1));

    var expectedJson = "{\"errors\":[{\"message\":\"Non-supported entity type: USER\",\"type\":\"-1\",\"code\":\"Validation error\",\"parameters\":null}],\"total_records\":1}";

    mockMvc.perform(post(format(ITEMS_CONTENT_UPDATE_UPLOAD_URL_TEMPLATE, UUID.randomUUID()))
        .headers(defaultHeaders())
        .content(updates))
      .andExpect(status().isBadRequest())
      .andExpect(content().json(expectedJson));
  }

  @Test
  @DisplayName("Post invalid content updates - BAD REQUEST")
  @SneakyThrows
  void shouldReturnBadRequestForInvalidContentUpdates() {
    var updates = new ObjectMapper().writeValueAsString(new ContentUpdateCollection()
      .contentUpdates(Collections.singletonList(new ContentUpdate()
        .option(ContentUpdate.OptionEnum.TEMPORARY_LOCATION)
        .action(ContentUpdate.ActionEnum.REPLACE_WITH)))
      .totalRecords(1));

    var expectedJson = "{\"errors\":[{\"message\":\"Invalid request body\",\"type\":\"-1\",\"code\":\"Validation error\",\"parameters\":[{\"key\":\"entityType\",\"value\":\"must not be null\"}]}],\"total_records\":1}";

    mockMvc.perform(post(format(ITEMS_CONTENT_UPDATE_UPLOAD_URL_TEMPLATE, UUID.randomUUID()))
      .headers(defaultHeaders())
      .content(updates))
      .andExpect(status().isBadRequest())
      .andExpect(content().json(expectedJson));
  }

  @Test
  @DisplayName("Post empty content updates - BAD REQUEST")
  @SneakyThrows
  void shouldReturnBadRequestForEmptyContentUpdates() {
    var updates = new ObjectMapper().writeValueAsString(new ContentUpdateCollection().entityType(ITEM).totalRecords(0));

    var expectedJson = "{\"errors\":[{\"message\":\"Invalid request body\",\"type\":\"-1\",\"code\":\"Validation error\",\"parameters\":[{\"key\":\"contentUpdates\",\"value\":\"size must be between 1 and 2147483647\"}]}],\"total_records\":1}";

    mockMvc.perform(post(format(ITEMS_CONTENT_UPDATE_UPLOAD_URL_TEMPLATE, UUID.randomUUID()))
        .headers(defaultHeaders())
        .content(updates))
      .andExpect(status().isBadRequest())
      .andExpect(content().string(expectedJson));
  }

  private JobCommand createBulkEditJobRequest(UUID id, ExportType exportType, EntityType entityType, IdentifierType identifierType) {
    JobCommand jobCommand = new JobCommand();
    jobCommand.setType(JobCommand.Type.START);
    jobCommand.setId(id);
    jobCommand.setName(exportType.toString());
    jobCommand.setDescription("Job description");
    jobCommand.setExportType(exportType);
    jobCommand.setIdentifierType(identifierType);
    jobCommand.setEntityType(entityType);

    Map<String, JobParameter> params = new HashMap<>();
    params.put("query", new JobParameter("(patronGroup==\"3684a786-6671-4268-8ed0-9db82ebca60b\") sortby personal.lastName"));
    String fileName;
    if (BULK_EDIT_IDENTIFIERS == exportType) {
      fileName = "src/test/resources/upload/barcodes.csv";
    } else {
      fileName = USER == entityType ? "src/test/resources/upload/user_data.csv" : "src/test/resources/upload/item_data.csv";
    }
    params.put(FILE_NAME, new JobParameter(fileName));
    jobCommand.setJobParameters(new JobParameters(params));
    return jobCommand;
  }

  private UserCollection buildUserCollection() {
    return new UserCollection()
      .addUsersItem(new User().barcode("123").patronGroup("3684a786-6671-4268-8ed0-9db82ebca60b"))
      .addUsersItem(new User().barcode("456").patronGroup("3684a786-6671-4268-8ed0-9db82ebca60b"))
      .addUsersItem(new User().barcode("789").patronGroup("3684a786-6671-4268-8ed0-9db82ebca60b"))
      .totalRecords(3);
  }

  private ItemCollection buildItemCollection() {
    return new ItemCollection()
      .addItemsItem(new Item().barcode("123"))
      .addItemsItem(new Item().barcode("456"))
      .addItemsItem(new Item().barcode("789"))
      .totalRecords(3);
  }

  private void verifyLocationUpdate(ItemCollection expectedItems, ItemCollection actualItems) {
    for (int i = 0; i < expectedItems.getItems().size(); i++) {
      assertThat(expectedItems.getItems().get(i).getId(), equalTo(actualItems.getItems().get(i).getId()));
      assertThat(expectedItems.getItems().get(i).getPermanentLocation(), equalTo(actualItems.getItems().get(i).getPermanentLocation()));
      assertThat(expectedItems.getItems().get(i).getTemporaryLocation(), equalTo(actualItems.getItems().get(i).getTemporaryLocation()));
      assertThat(expectedItems.getItems().get(i).getEffectiveLocation(), equalTo(actualItems.getItems().get(i).getEffectiveLocation()));
    }
  }
}
