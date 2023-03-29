package org.folio.dew;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dew.domain.dto.ExportType.AUTH_HEADINGS_UPDATES;
import static org.folio.dew.domain.dto.ExportType.FAILED_LINKED_BIB_UPDATES;
import static org.folio.dew.domain.dto.JobParameterNames.AUTHORITY_CONTROL_FILE_NAME;
import static org.folio.dew.domain.dto.JobParameterNames.OUTPUT_FILES_IN_STORAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.springframework.batch.test.AssertFile.assertFileEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.File;
import java.time.LocalDate;
import java.util.UUID;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.folio.de.entity.JobCommand;
import org.folio.dew.config.kafka.KafkaService;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.JobParameterNames;
import org.folio.dew.domain.dto.authority.control.AuthorityControlExportConfig;
import org.folio.dew.repository.RemoteFilesStorage;
import org.folio.dew.service.FileNameResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.FileSystemResource;

@Log4j2
class AuthorityControlTest extends BaseBatchTest {

  private static final String EXPECTED_FAILED_LINKED_BIB_OUTPUT =
    "src/test/resources/output/authority_control/failed_linked_bib_updates.csv";
  private static final String EXPECTED_AUTH_HEADING_UPDATE_OUTPUT =
    "src/test/resources/output/authority_control/auth_heading_update.csv";
  private static final String EXPECTED_AUTH_HEADING_UPDATE_EMPTY_OUTPUT =
    "src/test/resources/output/authority_control/auth_heading_update_empty.csv";
  private static final String FILE_PATH = "mod-data-export-worker/authority_control_export/diku/";
  @Autowired
  private Job getAuthHeadingJob;
  @Autowired
  private Job getFailedLinkedBibJob;
  @Autowired
  private FileNameResolver fileNameResolver;
  @Autowired
  private RemoteFilesStorage remoteFilesStorage;
  @SpyBean
  private KafkaService kafkaService;

  @Test
  @DisplayName("Run AuthHeadingJob export successfully")
  void authHeadingJobTest() throws Exception {
    var exportConfig = buildExportConfig("2023-01-01", "2023-12-01");

    final JobLauncherTestUtils testLauncher = createTestLauncher(getAuthHeadingJob);
    final JobParameters jobParameters = prepareJobParameters(AUTH_HEADINGS_UPDATES, exportConfig);

    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

    verifyFile(jobExecution, EXPECTED_AUTH_HEADING_UPDATE_OUTPUT);

    wireMockServer.verify(getRequestedFor(urlEqualTo(
      "/links/stats/authority?limit=2&action=UPDATE_HEADING&fromDate=2023-01-01T00%3A00Z&toDate=2023-12-01T23%3A59%3A59.999999999Z")));
    wireMockServer.verify(getRequestedFor(urlEqualTo(
      "/links/stats/authority?limit=2&action=UPDATE_HEADING&fromDate=2023-01-01T00%3A00Z&toDate=2023-08-01T12%3A00Z")));

    verifyJobEvent();
  }

  @Test
  @DisplayName("Run AuthHeadingJob export successfully")
  void authHeadingJobTest_whenNoStatsFound() throws Exception {
    var exportConfig = buildExportConfig("2022-01-01", "2022-12-01");

    final JobLauncherTestUtils testLauncher = createTestLauncher(getAuthHeadingJob);
    final JobParameters jobParameters = prepareJobParameters(AUTH_HEADINGS_UPDATES, exportConfig);

    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

    verifyFile(jobExecution, EXPECTED_AUTH_HEADING_UPDATE_EMPTY_OUTPUT);

    wireMockServer.verify(getRequestedFor(urlEqualTo(
      "/links/stats/authority?limit=2&action=UPDATE_HEADING&fromDate=2022-01-01T00%3A00Z&toDate=2022-12-01T23%3A59%3A59.999999999Z")));

    verifyJobEvent();
  }

  @Test
  @DisplayName("Run FailedLinkedBibJob export successfully")
  void failedLinkedBibJobTest() throws Exception {
    final JobLauncherTestUtils testLauncher = createTestLauncher(getFailedLinkedBibJob);
    final JobParameters jobParameters = prepareJobParameters(FAILED_LINKED_BIB_UPDATES, null);

    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

    verifyFile(jobExecution, EXPECTED_FAILED_LINKED_BIB_OUTPUT);

    wireMockServer.verify(getRequestedFor(urlEqualTo(
      "/links/stats/instance?limit=2&status=ERROR")));
    wireMockServer.verify(getRequestedFor(urlEqualTo(
      "/links/stats/instance?limit=2&status=ERROR&toDate=2023-08-01T12%3A00Z")));

    verifyJobEvent();
  }

  private void verifyFile(JobExecution jobExecution, String expectedFile) throws Exception {
    final ExecutionContext executionContext = jobExecution.getExecutionContext();
    final String fileInStorage = executionContext.getString(OUTPUT_FILES_IN_STORAGE);
    final String fileName = executionContext.getString(AUTHORITY_CONTROL_FILE_NAME);

    assertEquals(FILE_PATH + fileName, fileInStorage);
    verifyFileOutput(fileInStorage, expectedFile);
  }

  private void verifyFileOutput(String fileInStorage, String expectedFile) throws Exception {
    final String presignedUrl = remoteFilesStorage.objectToPresignedObjectUrl(fileInStorage);
    final FileSystemResource actualOutput = actualFileOutput(presignedUrl);
    FileSystemResource expectedOutput = new FileSystemResource(expectedFile);
    assertFileEquals(expectedOutput, actualOutput);
  }

  private void verifyJobEvent() {
    var jobCaptor = ArgumentCaptor.forClass(org.folio.de.entity.Job.class);
    Mockito.verify(kafkaService, times(2)).send(eq(KafkaService.Topic.JOB_UPDATE), anyString(), jobCaptor.capture());

    var job = jobCaptor.getValue();
    final String filePath = job.getFiles().get(0);
    final String fileName = job.getFileNames().get(0);

    assertEquals(FILE_PATH + fileName, filePath);
  }

  @SneakyThrows
  private AuthorityControlExportConfig buildExportConfig(String fromDate, String toDate) {
    var exportConfig = new AuthorityControlExportConfig();
    exportConfig.setFromDate(LocalDate.parse(fromDate));
    exportConfig.setToDate(LocalDate.parse(toDate));
    return exportConfig;
  }

  private JobParameters prepareJobParameters(ExportType exportType, AuthorityControlExportConfig exportConfig)
    throws JsonProcessingException {
    String jobId = UUID.randomUUID().toString();
    var paramBuilder = new JobParametersBuilder();

    paramBuilder.addString(JobParameterNames.JOB_ID, jobId);
    if (exportConfig != null) {
      paramBuilder.addString("authorityControlExportConfig", objectMapper.writeValueAsString(exportConfig));
    }
    String workDir =
      System.getProperty("java.io.tmpdir")
        + File.separator
        + springApplicationName
        + File.separator;
    var jobParameters = paramBuilder.toJobParameters();
    var jobCommand = new JobCommand();
    jobCommand.setJobParameters(jobParameters);
    jobCommand.setExportType(exportType);
    final String outputFile = fileNameResolver.resolve(jobCommand, workDir, jobId);
    paramBuilder.addString(JobParameterNames.TEMP_OUTPUT_FILE_PATH, outputFile);

    return paramBuilder.toJobParameters();
  }
}
