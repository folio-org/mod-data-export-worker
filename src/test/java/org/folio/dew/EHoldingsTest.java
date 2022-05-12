package org.folio.dew;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.batch.test.AssertFile.assertFileEquals;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.JobParameterNames;

class EHoldingsTest extends BaseBatchTest {

  @Autowired
  private Job getEHoldingsJob;

  private final static String EXPECTED_CIRCULATION_OUTPUT = "src/test/resources/output/eholdings_export.csv";

  @Test
  @DisplayName("Run EHoldingsJob successfully")
  void eHoldingsJobTest() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(getEHoldingsJob);

    final JobParameters jobParameters = prepareJobParameters();
    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    verifyFileOutput(jobExecution);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
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
    params.put("recordId", new JobParameter("1-22-333"));
    params.put("recordType", new JobParameter("RESOURCE"));
    params.put("packageFields", new JobParameter("titleId"));
    params.put("titleFields", new JobParameter("titleName"));

    String jobId = UUID.randomUUID().toString();
    params.put(JobParameterNames.JOB_ID, new JobParameter(jobId));

    Date now = new Date();
    String workDir =
      System.getProperty("java.io.tmpdir")
        + springApplicationName
        + File.separator;
    final String outputFile =
      String.format(
        "%s%s_%tF_%tH%tM%tS_%s",
        workDir, ExportType.E_HOLDINGS, now, now, now, now, jobId);
    params.put(JobParameterNames.TEMP_OUTPUT_FILE_PATH, new JobParameter(outputFile));

    return new JobParameters(params);
  }

}
