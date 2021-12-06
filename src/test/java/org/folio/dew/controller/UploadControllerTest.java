package org.folio.dew.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import lombok.SneakyThrows;
import org.folio.des.domain.dto.EntityType;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.IdentifierType;
import org.folio.des.domain.dto.JobCommand;
import org.folio.dew.BaseBatchTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

class UploadControllerTest extends BaseBatchTest {
  private static final String URL_TEMPLATE = "/bulk-edit/%s/upload";
  @Test
  @DisplayName("Upload JobCommand successfully")
  @SneakyThrows
  void shouldLaunchJobOnFileUpload() {
    var jobId = UUID.randomUUID();
    service.addBulkEditJobCommand(createBulkEditJobRequest(jobId));

    var headers = defaultHeaders();

    var bytes = new FileInputStream("src/test/resources/upload/barcodes.csv").readAllBytes();
    var file = new MockMultipartFile("file", "barcodes.csv", MediaType.TEXT_PLAIN_VALUE, bytes);

    mockMvc.perform(multipart(String.format(URL_TEMPLATE, jobId))
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
    service.addBulkEditJobCommand(createBulkEditJobRequest(jobId));

    var headers = defaultHeaders();

    var file = new MockMultipartFile("file", "barcodes.csv", MediaType.TEXT_PLAIN_VALUE, new byte[]{});

    mockMvc.perform(multipart(String.format(URL_TEMPLATE, jobId))
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

    mockMvc.perform(multipart(String.format(URL_TEMPLATE, jobId))
        .file(file)
        .headers(headers))
      .andExpect(status().isNotFound());
  }

  private JobCommand createBulkEditJobRequest(UUID id) {
    JobCommand jobCommand = new JobCommand();
    jobCommand.setType(JobCommand.Type.START);
    jobCommand.setId(id);
    jobCommand.setName(ExportType.BULK_EDIT_IDENTIFIERS.toString());
    jobCommand.setDescription("Job description");
    jobCommand.setExportType(ExportType.BULK_EDIT_IDENTIFIERS);
    jobCommand.setIdentifierType(IdentifierType.BARCODE);
    jobCommand.setEntityType(EntityType.USER);

    Map<String, JobParameter> params = new HashMap<>();
    params.put("query", new JobParameter(""));
    jobCommand.setJobParameters(new JobParameters(params));
    return jobCommand;
  }
}
