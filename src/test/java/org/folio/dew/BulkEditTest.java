package org.folio.dew;

import org.folio.des.domain.JobParameterNames;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.batch.test.AssertFile.assertFileEquals;

class BulkEditTest extends BaseBatchTest {

  @Autowired private Job bulkEditJob;

  private final static String EXPECTED_BULK_EDIT_OUTPUT = "src/test/resources/output/bulk_edit_identifiers_output.csv";

  @Test
  @DisplayName("Run bulk-edit (identifiers) successfully")
  void circulationLogJobTest() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(bulkEditJob);

    final JobParameters jobParameters = prepareJobParameters();
    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    verifyFileOutput(jobExecution);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

    // check if caching works
    wireMockServer.verify(1, getRequestedFor(urlEqualTo("/groups/3684a786-6671-4268-8ed0-9db82ebca60b")));
  }

  private void verifyFileOutput(JobExecution jobExecution) throws Exception {
    final ExecutionContext executionContext = jobExecution.getExecutionContext();
    final String fileInStorage = (String) executionContext.get("outputFilesInStorage");

    final FileSystemResource actualResult = actualFileOutput(fileInStorage);
    FileSystemResource expectedCharges = new FileSystemResource(EXPECTED_BULK_EDIT_OUTPUT);
    assertFileEquals(expectedCharges, actualResult);
  }

  private JobParameters prepareJobParameters() {
    String workDir =
      System.getProperty("java.io.tmpdir")
        + File.separator
        + springApplicationName
        + File.separator;

    Map<String, JobParameter> params = new HashMap<>();
    params.put("identifiersFileName", new JobParameter("src/test/resources/upload/barcodes.csv"));
    params.put(JobParameterNames.TEMP_OUTPUT_FILE_PATH, new JobParameter(workDir + "out.csv"));

    String jobId = UUID.randomUUID().toString();
    params.put(JobParameterNames.JOB_ID, new JobParameter(jobId));

    return new JobParameters(params);
  }

}
