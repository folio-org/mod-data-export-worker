package org.folio.dew.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.folio.de.entity.JobCommand;
import org.folio.dew.BaseBatchTest;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.folio.dew.controller.UploadController.IDENTIFIERS_FILE_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UploadControllerTest extends BaseBatchTest {
  private static final String UPLOAD_URL_TEMPLATE = "/bulk-edit/%s/upload";
  private static final String PREVIEW_URL_TEMPLATE = "/bulk-edit/%s/preview";
  public static final String LIMIT = "limit";

  @Autowired
  private ObjectMapper mapper;

  @MockBean
  private UserClient client;

  @SneakyThrows
  @ParameterizedTest
  @CsvSource({"BULK_EDIT_IDENTIFIERS,barcode==(123 OR 456 OR 789)",
    "BULK_EDIT_QUERY,(patronGroup==\"3684a786-6671-4268-8ed0-9db82ebca60b\") sortby personal.lastName"})
  void shouldReturnCompletePreview(String exportType, String query) {

    when(client.getUserByQuery(query, 3)).thenReturn(buildUserCollection());

    var jobId = UUID.randomUUID();
    service.addBulkEditJobCommand(createBulkEditJobRequest(jobId, ExportType.fromValue(exportType)));

    var headers = defaultHeaders();

    var response = mockMvc.perform(get(String.format(PREVIEW_URL_TEMPLATE, jobId))
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
    service.addBulkEditJobCommand(createBulkEditJobRequest(jobId, ExportType.fromValue(exportType)));

    var headers = defaultHeaders();

    var response = mockMvc.perform(get(String.format(PREVIEW_URL_TEMPLATE, jobId))
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
    service.addBulkEditJobCommand(createBulkEditJobRequest(jobId, ExportType.ORDERS_EXPORT));

    var headers = defaultHeaders();

    mockMvc.perform(get(String.format(PREVIEW_URL_TEMPLATE, jobId))
        .headers(headers)
        .queryParam(LIMIT, String.valueOf(2)))
      .andExpect(status().isBadRequest());
  }

  @SneakyThrows
  @Test
  void shouldReturnErroJobNotFound() {

    var headers = defaultHeaders();

    mockMvc.perform(get(String.format(PREVIEW_URL_TEMPLATE, UUID.randomUUID()))
        .headers(headers)
        .queryParam(LIMIT, String.valueOf(2)))
      .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Launch job on upload file with identifiers successfully")
  @SneakyThrows
  void shouldLaunchJobOnIdentifiersFileUpload() {
    var jobId = UUID.randomUUID();
    service.addBulkEditJobCommand(createBulkEditJobRequest(jobId, ExportType.BULK_EDIT_IDENTIFIERS));

    var headers = defaultHeaders();

    var bytes = new FileInputStream("src/test/resources/upload/barcodes.csv").readAllBytes();
    var file = new MockMultipartFile("file", "barcodes.csv", MediaType.TEXT_PLAIN_VALUE, bytes);

    mockMvc.perform(multipart(String.format(UPLOAD_URL_TEMPLATE, jobId))
      .file(file)
      .headers(headers))
      .andExpect(status().isOk());

    verify(exportJobManager, times(1)).launchJob(any());
  }

  @Test
  @DisplayName("Upload empty file - BAD REQUEST")
  @SneakyThrows
  void shouldReturnBadRequestWhenIdentifiersFileIsEmpty() {
    var jobId = UUID.randomUUID();
    service.addBulkEditJobCommand(createBulkEditJobRequest(jobId, ExportType.BULK_EDIT_IDENTIFIERS));

    var headers = defaultHeaders();

    var file = new MockMultipartFile("file", "barcodes.csv", MediaType.TEXT_PLAIN_VALUE, new byte[]{});

    mockMvc.perform(multipart(String.format(UPLOAD_URL_TEMPLATE, jobId))
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

    mockMvc.perform(multipart(String.format(UPLOAD_URL_TEMPLATE, jobId))
        .file(file)
        .headers(headers))
      .andExpect(status().isNotFound());
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
    params.put(IDENTIFIERS_FILE_NAME, new JobParameter("src/test/resources/upload/barcodes.csv"));
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
