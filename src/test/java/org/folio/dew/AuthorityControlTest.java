package org.folio.dew;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.folio.de.entity.JobCommand;
import org.folio.dew.config.kafka.KafkaService;
import org.folio.dew.domain.dto.AuthorityControlExportConfig;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.JobParameterNames;
import org.folio.dew.repository.RemoteFilesStorage;
import org.folio.dew.service.FileNameResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dew.domain.dto.JobParameterNames.AUTHORITY_CONTROL_FILE_NAME;
import static org.folio.dew.domain.dto.JobParameterNames.OUTPUT_FILES_IN_STORAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.springframework.batch.test.AssertFile.assertFileEquals;

@Log4j2
class AuthorityControlTest extends BaseBatchTest {
  @Autowired
  private Job getAuthHeadingJob;
  @Autowired
  private FileNameResolver fileNameResolver;
  @Autowired
  private RemoteFilesStorage remoteFilesStorage;
  @SpyBean
  private KafkaService kafkaService;

  private static final String EXPECTED_AUTHORITY_STAT_OUTPUT = "src/test/resources/output/auth_heading_update.csv";
  private static final String FILE_PATH = "mod-data-export-worker/authority_control_export/diku/";
  private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

  @Test
  @DisplayName("Run AuthorityControlJob export successfully")
  void authorityControlJobTest() throws Exception {
    var exportConfig = buildExportConfig("2023-01-01T12:00:00Z", "2023-12-01T12:00:00Z");

    final JobLauncherTestUtils testLauncher = createTestLauncher(getAuthHeadingJob);
    final JobParameters jobParameters = prepareJobParameters(exportConfig);

    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

    verifyFile(jobExecution, EXPECTED_AUTHORITY_STAT_OUTPUT);

    wireMockServer.verify(getRequestedFor(urlEqualTo(
      "/links/authority/stats?limit=2&action=UPDATE_HEADING&fromDate=2023-01-01T12%3A00%3A00Z&toDate=2023-12-01T12%3A00%3A00Z")));
    wireMockServer.verify(getRequestedFor(urlEqualTo(
      "/links/authority/stats?limit=2&action=UPDATE_HEADING&fromDate=2023-01-01T12%3A00%3A00Z&toDate=2023-08-01T12%3A00%3A00Z")));

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
  private AuthorityControlExportConfig buildExportConfig(String from, String to) {
    var exportConfig = new AuthorityControlExportConfig();
    exportConfig.setFromDate(dateFormat.parse(from));
    exportConfig.setToDate(dateFormat.parse(to));
    return exportConfig;
  }

  private JobParameters prepareJobParameters(AuthorityControlExportConfig exportConfig)
    throws JsonProcessingException {
    String jobId = UUID.randomUUID().toString();
    var paramBuilder = new JobParametersBuilder();

    paramBuilder.addString(JobParameterNames.JOB_ID, jobId);
    paramBuilder.addString("authorityControlExportConfig", objectMapper.writeValueAsString(exportConfig));

    String workDir =
      System.getProperty("java.io.tmpdir")
        + File.separator
        + springApplicationName
        + File.separator;
    var jobParameters = paramBuilder.toJobParameters();
    var jobCommand = new JobCommand();
    jobCommand.setJobParameters(jobParameters);
    jobCommand.setExportType(ExportType.AUTH_HEADINGS_UPDATES);
    final String outputFile = fileNameResolver.resolve(jobCommand, workDir, jobId);
    paramBuilder.addString(JobParameterNames.TEMP_OUTPUT_FILE_PATH, outputFile);

    return paramBuilder.toJobParameters();
  }
}
