package org.folio.dew;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.folio.des.domain.JobParameterNames;
import org.folio.des.domain.dto.ExportType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;

class CirculationLogTest extends BaseBatchTest {

  @Autowired private Job getCirculationLogJob;

  @Test
  @DisplayName("Run CirculationLogJob successfully")
  void circulationLogJobTest() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(getCirculationLogJob);

    final JobParameters jobParameters = prepareJobParameters();
    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

    wireMockServer.verify(
      getRequestedFor(
        urlEqualTo(
          "/audit-data/circulation/logs?query&offset=0&limit=1")));
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
