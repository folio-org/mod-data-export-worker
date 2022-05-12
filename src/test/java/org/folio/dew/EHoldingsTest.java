package org.folio.dew;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.batch.test.AssertFile.assertFileEquals;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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

import org.folio.dew.domain.dto.EHoldingsResourceExportFormat;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.JobParameterNames;

class EHoldingsTest extends BaseBatchTest {

  @Autowired
  private Job getEHoldingsJob;

  private final static String RESOURCE_ID = "1-22-333";
  private final static String PACKAGE_ID = "1-22";
  private final static String EXPECTED_RESOURCE_OUTPUT = "src/test/resources/output/eholdings_resource_export.csv";
  private final static String EXPECTED_PACKAGE_OUTPUT = "src/test/resources/output/eholdings_package_export.csv";

  @Test
  @DisplayName("Run EHoldingsJob export resource successfully")
  void eHoldingsJobResourceTest() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(getEHoldingsJob);

    final JobParameters jobParameters = prepareJobParameters(RESOURCE_ID, "RESOURCE");
    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    verifyFileOutput(jobExecution, EXPECTED_RESOURCE_OUTPUT);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
  }

  @Test
  @DisplayName("Run EHoldingsJob export package successfully")
  void eHoldingsJobPackageTest() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(getEHoldingsJob);

    final JobParameters jobParameters = prepareJobParameters(PACKAGE_ID, "PACKAGE");
    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    verifyFileOutput(jobExecution, EXPECTED_PACKAGE_OUTPUT);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
  }

  private void verifyFileOutput(JobExecution jobExecution, String expectedFile) throws Exception {
    final ExecutionContext executionContext = jobExecution.getExecutionContext();
    final String fileInStorage = (String) executionContext.get("outputFilesInStorage");

    final FileSystemResource actualChargeFeesFinesOutput = actualFileOutput(fileInStorage);
    FileSystemResource expectedCharges = new FileSystemResource(expectedFile);
    assertFileEquals(expectedCharges, actualChargeFeesFinesOutput);
  }

  private JobParameters prepareJobParameters(String id, String recordType) {
    Map<String, JobParameter> params = new HashMap<>();
    params.put("recordId", new JobParameter(id));
    params.put("recordType", new JobParameter(recordType));
    params.put("titleFields", new JobParameter(getClassFields(EHoldingsResourceExportFormat.class)));
    params.put("packageFields", new JobParameter(""));
    params.put("titleFilters", new JobParameter("filter[name]=*"));

    String jobId = UUID.randomUUID().toString();
    params.put(JobParameterNames.JOB_ID, new JobParameter(jobId));

    Date now = new Date();
    String workDir =
      System.getProperty("java.io.tmpdir")
        //     + File.separator
        + springApplicationName
        + File.separator;
    final String outputFile =
      String.format(
        "%s%s_%tF_%tH%tM%tS_%s",
        workDir, ExportType.E_HOLDINGS, now, now, now, now, jobId);
    params.put(JobParameterNames.TEMP_OUTPUT_FILE_PATH, new JobParameter(outputFile));

    return new JobParameters(params);
  }

  private String getClassFields(Class clazz) {
    return Arrays.stream(clazz.getDeclaredFields())
      .map(Field::getName)
      .collect(Collectors.joining(","));
  }
}
