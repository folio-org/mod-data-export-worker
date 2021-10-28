package org.folio.dew;

import org.folio.des.domain.JobParameterNames;
import org.folio.des.domain.dto.ExportType;
import org.folio.dew.domain.dto.CirculationLogExportFormat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.batch.test.AssertFile.assertFileEquals;

class CirculationLogTest extends BaseBatchTest {

  @Autowired private Job getCirculationLogJob;

  private final static String EXPECTED_CIRCULATION_OUTPUT = "src/test/resources/output/circulation_export.csv";

  @Test
  @DisplayName("Run CirculationLogJob successfully")
  void circulationLogJobTest() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(getCirculationLogJob);

    final JobParameters jobParameters = prepareJobParameters();
    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    verifyFileOutput(jobExecution);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

    wireMockServer.verify(
      getRequestedFor(
        urlEqualTo(
          "/audit-data/circulation/logs?query&offset=0&limit=1")));
  }

  @Test
  @DisplayName("Check that date setting in 24 hours format instead of 12h and test pass successfully")
  void successfulSetDateIn24hFormatInsteadOf12hTest() throws ParseException {
    CirculationLogExportFormat circulationLogExportFormat = new CirculationLogExportFormat();
    String testDate = "2000-12-31 23:59";

    var oldDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm");
    circulationLogExportFormat.setDate(oldDateFormat.format(oldDateFormat.parse(testDate)));
    String before = circulationLogExportFormat.getDate();

    var newDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    circulationLogExportFormat.setDate(newDateFormat.format(newDateFormat.parse(testDate)));
    String after = circulationLogExportFormat.getDate();

    Assertions.assertNotEquals(after, before);
  }

  private void verifyFileOutput(JobExecution jobExecution) throws Exception {
    final ExecutionContext executionContext = jobExecution.getExecutionContext();
    final String fileInStorage = (String) executionContext.get("outputFilesInStorage");

    final FileSystemResource actualChargeFeesFinesOutput = actualFileOutput(fileInStorage);
    FileSystemResource expectedCharges = new FileSystemResource(EXPECTED_CIRCULATION_OUTPUT);
    assertFileEquals(expectedCharges, actualChargeFeesFinesOutput);
  }

  private JobParameters prepareJobParameters() {
    Map<String, JobParameter> params = new HashMap<>();
    params.put("query", new JobParameter(""));

    String jobId = UUID.randomUUID().toString();
    params.put(JobParameterNames.JOB_ID, new JobParameter(jobId));

    Date now = new Date();
    String workDir =
        System.getProperty("java.io.tmpdir")
            + File.separator
            + springApplicationName
            + File.separator;
    params.put(
        JobParameterNames.TEMP_OUTPUT_FILE_PATH,
        new JobParameter(
            String.format(
                "%s%s_%tF_%tH%tM%tS_%s",
                workDir, ExportType.CIRCULATION_LOG, now, now, now, now, jobId)));

    return new JobParameters(params);
  }

}
