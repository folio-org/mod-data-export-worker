package org.folio.dew.controller;

import static java.lang.String.format;
import static org.folio.dew.domain.dto.EntityType.USER;
import static org.folio.dew.domain.dto.ExportType.BULK_EDIT_IDENTIFIERS;
import static org.folio.dew.domain.dto.ExportType.BULK_EDIT_UPDATE;
import static org.folio.dew.domain.dto.IdentifierType.BARCODE;
import static org.folio.dew.domain.dto.JobParameterNames.TEMP_OUTPUT_FILE_PATH;
import static org.folio.dew.domain.dto.JobParameterNames.UPDATED_FILE_NAME;
import static org.folio.dew.utils.Constants.FILE_NAME;
import static org.folio.dew.utils.Constants.UPDATED_PREFIX;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.SneakyThrows;
import org.apache.commons.io.FilenameUtils;
import org.folio.de.entity.JobCommand;
import org.folio.de.entity.JobCommandType;
import org.folio.dew.BaseBatchTest;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.EntityType;
import org.folio.dew.domain.dto.ErrorType;
import org.folio.dew.domain.dto.Errors;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.IdentifierType;
import org.folio.dew.domain.dto.Metadata;
import org.folio.dew.domain.dto.Personal;
import org.folio.dew.domain.dto.User;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.error.FileOperationException;
import org.folio.dew.repository.LocalFilesStorage;
import org.folio.dew.service.BulkEditProcessingErrorsService;
import org.folio.dew.service.JobCommandsReceiverService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

class BulkEditControllerTest extends BaseBatchTest {
  private static final String ERRORS_URL_TEMPLATE = "/bulk-edit/%s/errors";
  private static final String UPLOAD_URL_TEMPLATE = "/bulk-edit/%s/upload";
  private static final String USER_DATA = "src/test/resources/upload/user_data.csv";
  private static final String ITEM_DATA = "src/test/resources/upload/item_data.csv";
  public static final String LIMIT = "limit";
  private static final UUID JOB_ID = UUID.randomUUID();

  @MockBean
  private UserClient userClient;

  @MockBean
  private JobCommandsReceiverService jobCommandsReceiverService;

  @Autowired
  private LocalFilesStorage localFilesStorage;

  @Autowired
  private BulkEditProcessingErrorsService bulkEditProcessingErrorsService;

  @Test
  void shouldReturnErrorsPreview() throws Exception {

    var jobId = JOB_ID;
    var jobCommand = createBulkEditJobRequest(jobId, BULK_EDIT_IDENTIFIERS, USER, BARCODE);
    when(jobCommandsReceiverService.getBulkEditJobCommandById(jobId.toString())).thenReturn(Optional.of(jobCommand));

    int numOfErrorLines = 3;
    int errorsPreviewLimit = 2;
    var reasonForError = new BulkEditException("Record not found", ErrorType.ERROR);
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

    bulkEditProcessingErrorsService.removeTemporaryErrorStorage();
  }

  @Test
  void shouldReturnEmptyErrorsForErrorsPreview() throws Exception {

    var jobId = JOB_ID;
    var jobCommand = createBulkEditJobRequest(jobId, BULK_EDIT_IDENTIFIERS, USER, BARCODE);
    when(jobCommandsReceiverService.getBulkEditJobCommandById(jobId.toString())).thenReturn(Optional.of(jobCommand));

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

    var jobId = JOB_ID;
    var expectedJson = String.format("{\"errors\":[{\"message\":\"JobCommand with id %s doesn't exist.\",\"type\":\"ERROR\",\"code\":\"Not found\",\"parameters\":null}],\"total_records\":1}", jobId);

    var headers = defaultHeaders();

    mockMvc.perform(get(format(ERRORS_URL_TEMPLATE, jobId))
        .headers(headers)
        .queryParam(LIMIT, String.valueOf(2)))
      .andExpect(status().isNotFound())
      .andExpect(content().json(expectedJson));
  }

  @Test
  @DisplayName("Launch job on upload file with identifiers successfully")
  @SneakyThrows
  void shouldLaunchJobAndReturnNumberOfRecordsOnIdentifiersFileUpload() {
    var jobId = UUID.randomUUID();
    var jobCommand = createBulkEditJobRequest(jobId, BULK_EDIT_IDENTIFIERS, USER, BARCODE);
    when(jobCommandsReceiverService.getBulkEditJobCommandById(jobId.toString())).thenReturn(Optional.of(jobCommand));

    var headers = defaultHeaders();

    var bytes = new FileInputStream("src/test/resources/upload/barcodes.csv").readAllBytes();
    var file = new MockMultipartFile("file", "barcodes.csv", MediaType.TEXT_PLAIN_VALUE, bytes);

    var result = mockMvc.perform(multipart(format(UPLOAD_URL_TEMPLATE, jobId))
        .file(file)
        .headers(headers))
      .andExpect(status().isOk())
      .andReturn();

    assertThat(result.getResponse().getContentAsString(), equalTo("3"));
    // Lets wait for async invocation to complete
    Thread.sleep(3000);
    verify(exportJobManagerSync, times(1)).launchJob(any());
  }

  @Test
  @DisplayName("Upload empty file - BAD REQUEST")
  @SneakyThrows
  void shouldReturnBadRequestWhenIdentifiersFileIsEmpty() {
    var jobId = JOB_ID;
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
  @SneakyThrows
  void shouldReturnNumberOfRowsInCSVFile() {
    when(userClient.getUserById("88a087b4-c3a1-485b-8a22-2fa8f7b661c4"))
      .thenReturn(new User().id("88a087b4-c3a1-485b-8a22-2fa8f7b661c4").username("User name").active(true).barcode("456")
        .departments(Set.of()).proxyFor(List.of()).personal(new Personal().lastName("").firstName("").middleName("")
          .preferredFirstName("").email("").phone("").mobilePhone("").addresses(List.of()).preferredContactTypeId("")).type("")
        .customFields(Map.of()).metadata(new Metadata().createdByUsername("abcd")));
    when(userClient.getUserById("88a087b4-c3a1-485b-8a22-2fa8f7b661c5"))
      .thenReturn(new User().id("88a087b4-c3a1-485b-8a22-2fa8f7b661c5").username("User name2").active(true).barcode("4567")
        .departments(Set.of()).proxyFor(List.of()).personal(new Personal().lastName("").firstName("").middleName("")
          .preferredFirstName("").email("").phone("").mobilePhone("").addresses(List.of()).preferredContactTypeId("")).type("")
        .customFields(Map.of()).metadata(new Metadata().createdByUsername("abcde")));
    when(userClient.getUserById("88a087b4-c3a1-485b-8a22-2fa8f7b661c6"))
      .thenReturn(new User().id("88a087b4-c3a1-485b-8a22-2fa8f7b661c6").username("User name3").active(true).barcode("45678")
        .departments(Set.of()).proxyFor(List.of()).personal(new Personal().lastName("").firstName("").middleName("")
          .preferredFirstName("").email("").phone("").mobilePhone("").addresses(List.of()).preferredContactTypeId("")).type("")
        .customFields(Map.of()).metadata(new Metadata().createdByUsername("abcdef")));

    var jobId = UUID.randomUUID();
    var jobCommand = createBulkEditJobRequest(jobId, BULK_EDIT_IDENTIFIERS, USER, BARCODE);

    when(jobCommandsReceiverService.getBulkEditJobCommandById(jobId.toString())).thenReturn(Optional.of(jobCommand));

    var headers = defaultHeaders();

    var bytes = new FileInputStream("src/test/resources/upload/bulk_edit_user_record_3_lines.csv").readAllBytes();
    var file = new MockMultipartFile("file", "bulk_edit_user_record_3_lines.csv", MediaType.TEXT_PLAIN_VALUE, bytes);

    var result = mockMvc.perform(multipart(format(UPLOAD_URL_TEMPLATE, jobId))
        .file(file)
        .headers(headers))
      .andExpect(status().isOk())
      .andReturn();

    // 3 lines loaded (+1 header because of BULK_EDIT_IDENTIFIERS job).
    assertThat(result.getResponse().getContentAsString(), equalTo("4"));
  }

  private JobCommand createBulkEditJobRequest(UUID id, ExportType exportType, EntityType entityType, IdentifierType identifierType) {
    JobCommand jobCommand = new JobCommand();
    jobCommand.setType(JobCommandType.START);
    jobCommand.setId(id);
    jobCommand.setName(exportType.toString());
    jobCommand.setDescription("Job description");
    jobCommand.setExportType(exportType);
    jobCommand.setIdentifierType(identifierType);
    jobCommand.setEntityType(entityType);

    var paramBuilder = new JobParametersBuilder();

    paramBuilder.addString("query", "(patronGroup==\"3684a786-6671-4268-8ed0-9db82ebca60b\") sortby personal.lastName");
    String fileName;
    if (BULK_EDIT_IDENTIFIERS == exportType) {
      fileName = "src/test/resources/upload/barcodes.csv";
      byte[] bytes = new byte[0];
      try {
        bytes = Files.readAllBytes(Path.of(fileName));
        localFilesStorage.write(fileName, bytes);
      } catch (IOException e) {
        throw new FileOperationException(e.getMessage());
      }
    } else {
      fileName = USER == entityType ? USER_DATA : ITEM_DATA;
      if (BULK_EDIT_UPDATE == exportType) {
        try {
          var bytes = Files.readAllBytes(Path.of(fileName));
          fileName = UPDATED_PREFIX + FilenameUtils.getName(fileName);
          localFilesStorage.write(fileName, bytes);
        } catch (Exception e) {
          throw new FileOperationException(e.getMessage());
        }
      }
    }
    paramBuilder.addString(FILE_NAME, fileName);
    paramBuilder.addString(TEMP_OUTPUT_FILE_PATH, fileName);
    paramBuilder.addString(UPDATED_FILE_NAME, fileName);
    jobCommand.setJobParameters(paramBuilder.toJobParameters());
    return jobCommand;
  }

}
