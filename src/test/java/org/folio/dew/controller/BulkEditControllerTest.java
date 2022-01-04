package org.folio.dew.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.folio.de.entity.JobCommand;
import org.folio.dew.BaseBatchTest;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.*;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.service.BulkEditProcessingErrorsService;
import org.folio.dew.service.BulkEditRollBackService;
import org.folio.tenant.domain.dto.Errors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.integration.launch.JobLaunchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.lang.String.format;
import static org.folio.dew.utils.Constants.FILE_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BulkEditControllerTest extends BaseBatchTest {
  private static final String UPLOAD_URL_TEMPLATE = "/bulk-edit/%s/upload";
  private static final String START_URL_TEMPLATE = "/bulk-edit/%s/start";
  private static final String PREVIEW_URL_TEMPLATE = "/bulk-edit/%s/preview";
  private static final String ERRORS_URL_TEMPLATE = "/bulk-edit/%s/errors";
  public static final String LIMIT = "limit";

  @Autowired
  private ObjectMapper mapper;

  @MockBean
  private BulkEditRollBackService bulkEditRollBackService;

  @MockBean
  private UserClient client;

  @Autowired
  private BulkEditProcessingErrorsService bulkEditProcessingErrorsService;

  @Test
  void shouldReturnErrorsPreview() throws Exception {

    var jobId = UUID.randomUUID();
    jobCommandsReceiverService.addBulkEditJobCommand(createBulkEditJobRequest(jobId, ExportType.BULK_EDIT_IDENTIFIERS));

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
  void shouldReturnJobNotFoundErrorForErrorsPreview() throws Exception {

    var jobId = UUID.randomUUID();
    jobCommandsReceiverService.addBulkEditJobCommand(createBulkEditJobRequest(jobId, ExportType.BULK_EDIT_IDENTIFIERS));

    var expectedErrorMsg = format("errors file for job id %s", jobId);

    var headers = defaultHeaders();

    mockMvc.perform(get(format(ERRORS_URL_TEMPLATE, jobId))
        .headers(headers)
        .queryParam(LIMIT, String.valueOf(2)))
      .andExpect(status().isInternalServerError())
      .andExpect(content().string(containsString(expectedErrorMsg)));
  }

  @Test
  void shouldReturnErrorsFileNotFoundErrorForErrorsPreview() throws Exception {

    var jobId = UUID.randomUUID();
    var expectedErrorMsg = format("JobCommand with id %s doesn't exist.", jobId);

    var headers = defaultHeaders();

    mockMvc.perform(get(format(ERRORS_URL_TEMPLATE, jobId))
        .headers(headers)
        .queryParam(LIMIT, String.valueOf(2)))
      .andExpect(status().isNotFound())
      .andExpect(content().string(expectedErrorMsg));
  }

  @SneakyThrows
  @ParameterizedTest
  @CsvSource({"BULK_EDIT_IDENTIFIERS,barcode==(123 OR 456 OR 789)",
    "BULK_EDIT_QUERY,(patronGroup==\"3684a786-6671-4268-8ed0-9db82ebca60b\") sortby personal.lastName"})
  void shouldReturnCompletePreview(String exportType, String query) {

    when(client.getUserByQuery(query, 3)).thenReturn(buildUserCollection());

    var jobId = UUID.randomUUID();
    jobCommandsReceiverService.addBulkEditJobCommand(createBulkEditJobRequest(jobId, ExportType.fromValue(exportType)));

    var headers = defaultHeaders();

    var response = mockMvc.perform(get(format(PREVIEW_URL_TEMPLATE, jobId))
        .headers(headers)
        .queryParam(LIMIT, String.valueOf(3)))
      .andExpect(status().isOk());

    var users = objectMapper.readValue(response.andReturn().getResponse().getContentAsString(), UserCollection.class);
    assertThat(users.getTotalRecords(), equalTo(3));
    assertThat(users.getUsers(), hasSize(3));
  }

  @SneakyThrows
  @ParameterizedTest
  @CsvSource({"BULK_EDIT_IDENTIFIERS,barcode==(123 OR 456)",
    "BULK_EDIT_QUERY,(patronGroup==\"3684a786-6671-4268-8ed0-9db82ebca60b\") sortby personal.lastName"})
  void shouldReturnCompletePreviewWithLimitControl(String exportType, String query) {

    when(client.getUserByQuery(query, 2)).thenReturn(buildUserCollection());

    var jobId = UUID.randomUUID();
    jobCommandsReceiverService.addBulkEditJobCommand(createBulkEditJobRequest(jobId, ExportType.fromValue(exportType)));

    var headers = defaultHeaders();

    var response = mockMvc.perform(get(format(PREVIEW_URL_TEMPLATE, jobId))
        .headers(headers)
        .queryParam(LIMIT, String.valueOf(2)))
      .andExpect(status().isOk());

    ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Long> limitCaptor = ArgumentCaptor.forClass(Long.class);
    verify(client).getUserByQuery(queryCaptor.capture(), limitCaptor.capture());
    assertThat(query, equalTo(queryCaptor.getValue()));
    assertThat(2L, equalTo(limitCaptor.getValue()));
  }

  @SneakyThrows
  @Test
  void shouldReturnErrorForInvalidExportType() {

    var jobId = UUID.randomUUID();
    jobCommandsReceiverService.addBulkEditJobCommand(createBulkEditJobRequest(jobId, ExportType.ORDERS_EXPORT));

    var headers = defaultHeaders();

    mockMvc.perform(get(format(PREVIEW_URL_TEMPLATE, jobId))
        .headers(headers)
        .queryParam(LIMIT, String.valueOf(2)))
      .andExpect(status().isBadRequest());
  }

  @SneakyThrows
  @Test
  void shouldReturnErroJobNotFound() {

    var headers = defaultHeaders();

    mockMvc.perform(get(format(PREVIEW_URL_TEMPLATE, UUID.randomUUID()))
        .headers(headers)
        .queryParam(LIMIT, String.valueOf(2)))
      .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Launch job on upload file with identifiers successfully")
  @SneakyThrows
  void shouldLaunchJobAndReturnNumberOfRecordsOnIdentifiersFileUpload() {
    var jobId = UUID.randomUUID();
    jobCommandsReceiverService.addBulkEditJobCommand(createBulkEditJobRequest(jobId, ExportType.BULK_EDIT_IDENTIFIERS));

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
  @DisplayName("Upload empty file - BAD REQUEST")
  @SneakyThrows
  void shouldReturnBadRequestWhenIdentifiersFileIsEmpty() {
    var jobId = UUID.randomUUID();
    jobCommandsReceiverService.addBulkEditJobCommand(createBulkEditJobRequest(jobId, ExportType.BULK_EDIT_IDENTIFIERS));

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
    jobCommand.setJobParameters(new JobParameters(new HashMap<String, JobParameter>()));

    var headers = defaultHeaders();

    when(jobCommandsReceiverService.getBulkEditJobCommandById(jobId.toString())).thenReturn(Optional.of(jobCommand));
    when(exportJobManager.launchJob(isA(JobLaunchRequest.class))).thenThrow(new JobExecutionException("Execution exception"));

    mockMvc.perform(multipart(format(START_URL_TEMPLATE, jobId))
      .headers(headers))
      .andExpect(status().isInternalServerError());
  }

  private JobCommand createBulkEditJobRequest(UUID id, ExportType exportType) {
    JobCommand jobCommand = new JobCommand();
    jobCommand.setType(JobCommand.Type.START);
    jobCommand.setId(id);
    jobCommand.setName(exportType.toString());
    jobCommand.setDescription("Job description");
    jobCommand.setExportType(exportType);
    jobCommand.setIdentifierType(IdentifierType.BARCODE);
    jobCommand.setEntityType(EntityType.USER);

    Map<String, JobParameter> params = new HashMap<>();
    params.put("query", new JobParameter("(patronGroup==\"3684a786-6671-4268-8ed0-9db82ebca60b\") sortby personal.lastName"));
    params.put(FILE_NAME, new JobParameter("src/test/resources/upload/barcodes.csv"));
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
}
