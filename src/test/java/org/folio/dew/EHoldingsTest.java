package org.folio.dew;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.batch.test.AssertFile.assertFileEquals;

import static org.folio.dew.domain.dto.EHoldingsExportConfig.RecordTypeEnum.PACKAGE;
import static org.folio.dew.domain.dto.EHoldingsExportConfig.RecordTypeEnum.RESOURCE;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
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

import org.folio.dew.domain.dto.EHoldingsExportConfig;
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

    final JobParameters jobParameters = prepareJobParameters(RESOURCE_ID, RESOURCE);
    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    verifyFileOutput(jobExecution, EXPECTED_RESOURCE_OUTPUT);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

    wireMockServer.verify(
      getRequestedFor(
        urlEqualTo(
          "/eholdings/packages/1-22?include=accessType")));
  }

  @Test
  @DisplayName("Run EHoldingsJob export package successfully")
  void eHoldingsJobPackageTest() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(getEHoldingsJob);

    final JobParameters jobParameters = prepareJobParameters(PACKAGE_ID, PACKAGE);
    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    verifyFileOutput(jobExecution, EXPECTED_PACKAGE_OUTPUT);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

    wireMockServer.verify(
      getRequestedFor(
        urlEqualTo(
          "/eholdings/packages/1-22/resources?filter[name]=*&count=1&page=1")));
  }

  private void verifyFileOutput(JobExecution jobExecution, String expectedFile) throws Exception {
    final ExecutionContext executionContext = jobExecution.getExecutionContext();
    final String fileInStorage = executionContext.getString("outputFilesInStorage");

    final FileSystemResource actualChargeFeesFinesOutput = actualFileOutput(fileInStorage);
    FileSystemResource expectedCharges = new FileSystemResource(expectedFile);
    assertFileEquals(expectedCharges, actualChargeFeesFinesOutput);
  }

  private JobParameters prepareJobParameters(String id, EHoldingsExportConfig.RecordTypeEnum recordType) throws JsonProcessingException {
    var eHoldingsExportConfig = new EHoldingsExportConfig();
    eHoldingsExportConfig.setRecordId(id);
    eHoldingsExportConfig.setRecordType(recordType);
    eHoldingsExportConfig.setTitleFields(getClassFields(EHoldingsResourceExportFormat.class));
    eHoldingsExportConfig.setPackageFields(Collections.emptyList());
    eHoldingsExportConfig.setTitleSearchFilters("filter[name]=*");

    Map<String, JobParameter> params = new HashMap<>();
    params.put("eHoldingsExportConfig", new JobParameter(objectMapper.writeValueAsString(eHoldingsExportConfig)));

    String jobId = UUID.randomUUID().toString();
    params.put(JobParameterNames.JOB_ID, new JobParameter(jobId));
    params.put("tenantId", new JobParameter(TENANT));

    Date now = new Date();
    String workDir =
      System.getProperty("java.io.tmpdir")
        + File.separator
        + springApplicationName
        + File.separator;
    final String outputFile =
      String.format(
        "%s%s_%tF_%tH%tM%tS_%s",
        workDir, ExportType.E_HOLDINGS, now, now, now, now, jobId);
    params.put(JobParameterNames.TEMP_OUTPUT_FILE_PATH, new JobParameter(outputFile));

    return new JobParameters(params);
  }

  private List<String> getClassFields(Class clazz) {
    return Arrays.stream(clazz.getDeclaredFields())
      .map(Field::getName)
      .collect(Collectors.toList());
  }
}
