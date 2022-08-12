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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.log4j.Log4j2;
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

import org.folio.de.entity.JobCommand;
import org.folio.dew.domain.dto.EHoldingsExportConfig;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.JobParameterNames;
import org.folio.dew.domain.dto.eholdings.EHoldingsResourceExportFormat;
import org.folio.dew.service.FileNameResolver;

@Log4j2
class EHoldingsTest extends BaseBatchTest {

  @Autowired
  private Job getEHoldingsJob;
  @Autowired
  private FileNameResolver fileNameResolver;

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
          "/eholdings/packages/1-22/resources?filter%5Bname%5D=*&page=1&count=1")));
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
    eHoldingsExportConfig.setTitleFields(getClassFields());
    eHoldingsExportConfig.setPackageFields(List.of("packageNotes", "packageAgreements"));
    eHoldingsExportConfig.setTitleSearchFilters("filter[name]=*&InvalidFilter");

    Map<String, JobParameter> params = new HashMap<>();
    params.put("eHoldingsExportConfig", new JobParameter(objectMapper.writeValueAsString(eHoldingsExportConfig)));

    String jobId = UUID.randomUUID().toString();
    params.put(JobParameterNames.JOB_ID, new JobParameter(jobId));

    String workDir =
      System.getProperty("java.io.tmpdir")
        + File.separator
        + springApplicationName
        + File.separator;
    var jobParameters = new JobParameters(params);
    var jobCommand = new JobCommand();
    jobCommand.setJobParameters(jobParameters);
    jobCommand.setExportType(ExportType.E_HOLDINGS);
    final String outputFile = fileNameResolver.resolve(jobCommand, workDir, jobId);
    params.put(JobParameterNames.TEMP_OUTPUT_FILE_PATH, new JobParameter(outputFile));

    return new JobParameters(params);
  }

  private List<String> getClassFields() {
    return Arrays.stream(EHoldingsResourceExportFormat.class.getDeclaredFields())
      .map(Field::getName)
      .filter(name -> !name.equals("packageNotes") && !name.equals("packageAgreements"))
      .collect(Collectors.toList());
  }
}
