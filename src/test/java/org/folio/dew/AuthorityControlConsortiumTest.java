package org.folio.dew;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dew.domain.dto.ExportType.AUTH_HEADINGS_UPDATES;
import static org.folio.dew.domain.dto.JobParameterNames.AUTHORITY_CONTROL_FILE_NAME;
import static org.folio.dew.domain.dto.JobParameterNames.OUTPUT_FILES_IN_STORAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.File;
import java.time.LocalDate;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.de.entity.JobCommand;
import org.folio.dew.config.kafka.KafkaService;
import org.folio.dew.domain.dto.JobParameterNames;
import org.folio.dew.domain.dto.authority.control.AuthorityControlExportConfig;
import org.folio.dew.repository.RemoteFilesStorage;
import org.folio.dew.service.FileNameResolver;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

class AuthorityControlConsortiumTest extends BaseBatchTest {

  private static final String EXPECTED_AUTH_HEADING_UPDATE_OUTPUT =
    "src/test/resources/output/authority_control/auth_heading_update_consortium.csv";
  private static final String EXPECTED_S3_FILE_PATH =
    "remote/mod-data-export-worker/authority_control_export/consortium/";
  @Autowired
  private Job getAuthHeadingJob;
  @Autowired
  private FileNameResolver fileNameResolver;
  @Autowired
  private RemoteFilesStorage remoteFilesStorage;
  @MockitoSpyBean
  private KafkaService kafkaService;

  @BeforeAll
  static void beforeAll() {
    setUpTenant("consortium");
  }

  @Test
  @DisplayName("Run AuthHeadingJob export in the consortium tenant successfully")
  void authHeadingJobTest() throws Exception {
    var exportConfig = buildExportConfig("2024-01-01", "2024-12-01");
    var testLauncher = createTestLauncher(getAuthHeadingJob);
    var jobParameters = prepareJobParameters(exportConfig);

    var jobExecution = testLauncher.launchJob(jobParameters);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

    verifyFile(jobExecution);
    wireMockServer.verify(getRequestedFor(urlEqualTo(
      "/links/stats/authority?limit=2&action=UPDATE_HEADING&fromDate=2024-01-01T00%3A00Z&toDate=2024-12-01T23%3A59%3A59.999999999Z")));
    wireMockServer.verify(getRequestedFor(urlEqualTo(
      "/links/stats/authority?limit=2&action=UPDATE_HEADING&fromDate=2024-01-01T00%3A00Z&toDate=2024-08-01T12%3A00Z")));
    verifyJobEvent();
  }

  private void verifyFile(JobExecution jobExecution) throws Exception {
    var executionContext = jobExecution.getExecutionContext();
    var fileInStorage = executionContext.getString(OUTPUT_FILES_IN_STORAGE);
    var fileName = executionContext.getString(AUTHORITY_CONTROL_FILE_NAME);

    assertEquals(EXPECTED_S3_FILE_PATH + fileName, fileInStorage);
    verifyFileOutput(fileInStorage);
  }

  private void verifyFileOutput(String fileInStorage) throws Exception {
    var presignedUrl = remoteFilesStorage.objectToPresignedObjectUrl(fileInStorage);
    var actualOutput = actualFileOutput(presignedUrl);
    var expectedOutput = new FileSystemResource(EXPECTED_AUTH_HEADING_UPDATE_OUTPUT);
    assertTrue(FileUtils.contentEqualsIgnoreEOL(expectedOutput.getFile(), actualOutput.getFile(), "UTF-8")
      , "Files are not identical!");
  }

  private void verifyJobEvent() {
    var jobCaptor = ArgumentCaptor.forClass(org.folio.de.entity.Job.class);
    Mockito.verify(kafkaService, times(2)).send(eq(KafkaService.Topic.JOB_UPDATE), anyString(), jobCaptor.capture());

    var job = jobCaptor.getValue();
    var filePath = job.getFiles().getFirst();
    var fileName = job.getFileNames().getFirst();

    assertEquals(EXPECTED_S3_FILE_PATH + fileName, filePath);
  }

  @SneakyThrows
  private AuthorityControlExportConfig buildExportConfig(String fromDate, String toDate) {
    var exportConfig = new AuthorityControlExportConfig();
    exportConfig.setFromDate(LocalDate.parse(fromDate));
    exportConfig.setToDate(LocalDate.parse(toDate));
    return exportConfig;
  }

  private JobParameters prepareJobParameters(AuthorityControlExportConfig exportConfig)
    throws JsonProcessingException {
    var jobId = UUID.randomUUID().toString();
    var paramBuilder = new JobParametersBuilder();

    paramBuilder.addString(JobParameterNames.JOB_ID, jobId);
    if (exportConfig != null) {
      paramBuilder.addString("authorityControlExportConfig", objectMapper.writeValueAsString(exportConfig));
    }
    var workDir =
      System.getProperty("java.io.tmpdir")
        + File.separator
        + springApplicationName
        + File.separator;
    var jobParameters = paramBuilder.toJobParameters();
    var jobCommand = new JobCommand();
    jobCommand.setJobParameters(jobParameters);
    jobCommand.setExportType(AUTH_HEADINGS_UPDATES);
    var outputFile = fileNameResolver.resolve(jobCommand, workDir, jobId);
    paramBuilder.addString(JobParameterNames.TEMP_OUTPUT_FILE_PATH, outputFile);

    return paramBuilder.toJobParameters();
  }
}
