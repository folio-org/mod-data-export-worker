package org.folio.dew;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dew.domain.dto.EHoldingsExportConfig.RecordTypeEnum.PACKAGE;
import static org.folio.dew.domain.dto.EHoldingsExportConfig.RecordTypeEnum.RESOURCE;
import static org.folio.dew.domain.dto.JobParameterNames.E_HOLDINGS_FILE_NAME;
import static org.folio.dew.domain.dto.JobParameterNames.OUTPUT_FILES_IN_STORAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.springframework.batch.test.AssertFile.assertFileEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.folio.de.entity.EHoldingsPackage;
import org.folio.de.entity.EHoldingsResource;
import org.folio.de.entity.JobCommand;
import org.folio.dew.config.kafka.KafkaService;
import org.folio.dew.domain.dto.EHoldingsExportConfig;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.JobParameterNames;
import org.folio.dew.domain.dto.eholdings.EHoldingsPackageExportFormat;
import org.folio.dew.domain.dto.eholdings.EHoldingsResourceExportFormat;
import org.folio.dew.repository.EHoldingsPackageRepository;
import org.folio.dew.repository.EHoldingsResourceRepository;
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

@Log4j2
class EHoldingsTest extends BaseBatchTest {
  @Autowired
  private Job getEHoldingsJob;
  @Autowired
  private FileNameResolver fileNameResolver;
  @Autowired
  private EHoldingsPackageRepository packageRepository;
  @Autowired
  private EHoldingsResourceRepository resourceRepository;
  @Autowired
  private RemoteFilesStorage remoteFilesStorage;
  @SpyBean
  private KafkaService kafkaService;

  private final static String RESOURCE_ID = "1-22-333";
  private final static String PACKAGE_ID = "1-22";
  private final static String SINGLE_PACKAGE_ID = "1-23";
  private final static String PACKAGE_WITH_3_TITLES_ID = "1-21";
  private final static String PACKAGE_WITH_SAME_TITLE_NAMES_ID = "1-24";
  private final static String EXPECTED_RESOURCE_OUTPUT = "src/test/resources/output/eholdings_resource_export.csv";
  private final static String EXPECTED_PACKAGE_OUTPUT = "src/test/resources/output/eholdings_package_export.csv";
  private final static String EXPECTED_SINGLE_PACKAGE_OUTPUT =
    "src/test/resources/output/eholdings_single_package_export.csv";
  private final static String EXPECTED_PACKAGE_WITH_3_TITLES_OUTPUT =
    "src/test/resources/output/eholdings_package_export_with_3_titles.csv";
  private final static String EXPECTED_PACKAGE_WITH_SAME_TITLE_NAMES_OUTPUT =
    "src/test/resources/output/eholdings_package_export_with_same_title_names.csv";
  private static final String FILE_PATH = "mod-data-export-worker/e_holdings_export/diku/";

  @Test
  @DisplayName("Run EHoldingsJob export resource without provider load successfully")
  void eHoldingsJobResourceTest() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(getEHoldingsJob);
    var exportConfig = buildExportConfig(RESOURCE_ID, RESOURCE);
    exportConfig.getPackageFields().remove("providerLevelToken");

    final JobParameters jobParameters = prepareJobParameters(exportConfig);

    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    verifyFile(jobExecution, EXPECTED_RESOURCE_OUTPUT);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

    wireMockServer.verify(
      getRequestedFor(
        urlEqualTo(
          "/eholdings/packages/1-22?include=accessType")));

    var packages = packageRepository.findAll();
    var resources = resourceRepository.findAll();
    assertEquals(0, ((Collection<?>) packages).size());
    assertEquals(0, ((Collection<?>) resources).size());
    verifyJobEvent();
  }

  @Test
  @DisplayName("Run EHoldingsJob export package successfully")
  void eHoldingsJobPackageTest() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(getEHoldingsJob);
    var exportConfig = buildExportConfig(PACKAGE_ID, PACKAGE);

    final JobParameters jobParameters = prepareJobParameters(exportConfig);
    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    verifyFile(jobExecution, EXPECTED_PACKAGE_OUTPUT);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

    wireMockServer.verify(
      getRequestedFor(
        urlEqualTo(
          "/eholdings/packages/1-22/resources?filter%5Bname%5D=*&sort=name&page=1&count=1")));

    var packages = packageRepository.findAll();
    var resources = resourceRepository.findAll();
    assertEquals(0, ((Collection<?>) packages).size());
    assertEquals(0, ((Collection<?>) resources).size());
    verifyJobEvent();
  }

  @Test
  @DisplayName("Run EHoldingsJob export package with 3 titles successfully")
  void eHoldingsJobPackageWith3TitlesTest() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(getEHoldingsJob);
    var exportConfig = buildExportConfig(PACKAGE_WITH_3_TITLES_ID, PACKAGE);

    final JobParameters jobParameters = prepareJobParameters(exportConfig);
    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    verifyFile(jobExecution, EXPECTED_PACKAGE_WITH_3_TITLES_OUTPUT);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

    wireMockServer.verify(
      getRequestedFor(
        urlEqualTo(
          "/eholdings/packages/1-21/resources?filter%5Bname%5D=*&sort=name&page=1&count=1")));

    var packages = packageRepository.findAll();
    var resources = resourceRepository.findAll();
    assertEquals(0, ((Collection<?>) packages).size());
    assertEquals(0, ((Collection<?>) resources).size());
    verifyJobEvent();
  }

  @Test
  @DisplayName("Run EHoldingsJob export package without resources successfully")
  void eHoldingsJobSinglePackageTest() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(getEHoldingsJob);
    var exportConfig = buildExportConfig(SINGLE_PACKAGE_ID, PACKAGE);

    final JobParameters jobParameters = prepareJobParameters(exportConfig);
    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    verifyFile(jobExecution, EXPECTED_SINGLE_PACKAGE_OUTPUT);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

    wireMockServer.verify(
      getRequestedFor(
        urlEqualTo(
          "/eholdings/packages/1-23/resources?filter%5Bname%5D=*&sort=name&page=1&count=1")));

    var packages = packageRepository.findAll();
    var resources = resourceRepository.findAll();
    assertEquals(0, ((Collection<?>) packages).size());
    assertEquals(0, ((Collection<?>) resources).size());
    verifyJobEvent();
  }

  @Test
  @DisplayName("Run 2 EHoldingsJob export package successfully")
  void eHoldingsJobPackageConcurrentTest() throws Exception {
    populateOtherJobsDataInDatabase();

    JobLauncherTestUtils testLauncher = createTestLauncher(getEHoldingsJob);
    var exportConfig = buildExportConfig(PACKAGE_ID, PACKAGE);

    final JobParameters jobParameters = prepareJobParameters(exportConfig);
    JobExecution jobExecution = testLauncher.launchJob(jobParameters);
    verifyFile(jobExecution, EXPECTED_PACKAGE_OUTPUT);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

    wireMockServer.verify(
      getRequestedFor(
        urlEqualTo(
          "/eholdings/packages/1-22/resources?filter%5Bname%5D=*&sort=name&page=1&count=1")));

    var packages = packageRepository.findAll();
    var resources = resourceRepository.findAll();
    assertEquals(1, ((Collection<?>) packages).size());
    assertEquals(1, ((Collection<?>) resources).size());

    cleanJobDataInDatabase();
    verifyJobEvent();
  }

  @Test
  @DisplayName("Run EHoldingsJob export package with same title names successfully")
  void eHoldingsJobPackageWithSameTitleNamesTest() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(getEHoldingsJob);
    var exportConfig = buildExportConfig(PACKAGE_WITH_SAME_TITLE_NAMES_ID, PACKAGE);
    exportConfig.getTitleFields().removeAll(asList("titleAgreements", "titleNotes"));
    exportConfig.getPackageFields().removeAll(asList("packageAgreements", "packageNotes"));

    final JobParameters jobParameters = prepareJobParameters(exportConfig);
    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    verifyFile(jobExecution, EXPECTED_PACKAGE_WITH_SAME_TITLE_NAMES_OUTPUT);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

    wireMockServer.verify(
      getRequestedFor(
        urlEqualTo(
          "/eholdings/packages/1-24/resources?filter%5Bname%5D=*&sort=name&page=1&count=1")));

    var packages = packageRepository.findAll();
    var resources = resourceRepository.findAll();
    assertEquals(0, ((Collection<?>) packages).size());
    assertEquals(0, ((Collection<?>) resources).size());
    verifyJobEvent();
  }

  private void populateOtherJobsDataInDatabase(){
    var packageFromAnotherJob = new EHoldingsPackage();
    packageFromAnotherJob.setId("1-22");
    packageFromAnotherJob.setJobExecutionId(2L);
    packageRepository.save(packageFromAnotherJob);

    var resourceFromAnotherJob = new EHoldingsResource();
    resourceFromAnotherJob.setId("1-22-3334");
    resourceFromAnotherJob.setJobExecutionId(2L);
    resourceFromAnotherJob.setName("ABC of Diabetes (ABC Series)");
    resourceRepository.save(resourceFromAnotherJob);
  }

  private void cleanJobDataInDatabase(){
    packageRepository.deleteAll();
    resourceRepository.deleteAll();
  }

  private void verifyFile(JobExecution jobExecution, String expectedFile) throws Exception {
    final ExecutionContext executionContext = jobExecution.getExecutionContext();
    final String fileInStorage = executionContext.getString(OUTPUT_FILES_IN_STORAGE);
    final String fileName = executionContext.getString(E_HOLDINGS_FILE_NAME);

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

  private EHoldingsExportConfig buildExportConfig(String id, EHoldingsExportConfig.RecordTypeEnum recordType) {
    var eHoldingsExportConfig = new EHoldingsExportConfig();
    eHoldingsExportConfig.setRecordId(id);
    eHoldingsExportConfig.setRecordType(recordType);
    eHoldingsExportConfig.setTitleFields(getTitleFields());
    eHoldingsExportConfig.setPackageFields(getPackageFields());
    eHoldingsExportConfig.setTitleSearchFilters("filter[name]=*&InvalidFilter");
    return eHoldingsExportConfig;
  }

  private JobParameters prepareJobParameters(EHoldingsExportConfig eHoldingsExportConfig)
    throws JsonProcessingException {
    String jobId = UUID.randomUUID().toString();
    var paramsBuilder = new JobParametersBuilder();

    paramsBuilder.addString(JobParameterNames.JOB_ID, jobId);
    paramsBuilder.addString("eHoldingsExportConfig", objectMapper.writeValueAsString(eHoldingsExportConfig));

    String workDir =
      System.getProperty("java.io.tmpdir")
        + File.separator
        + springApplicationName
        + File.separator;
    var jobParameters = paramsBuilder.toJobParameters();
    var jobCommand = new JobCommand();
    jobCommand.setJobParameters(jobParameters);
    jobCommand.setExportType(ExportType.E_HOLDINGS);
    final String outputFile = fileNameResolver.resolve(jobCommand, workDir, jobId);
    paramsBuilder.addString(JobParameterNames.TEMP_OUTPUT_FILE_PATH, outputFile);

    return paramsBuilder.toJobParameters();
  }

  private List<String> getTitleFields() {
    return convertFields(EHoldingsResourceExportFormat.class.getDeclaredFields());
  }

  private List<String> getPackageFields() {
    return convertFields(EHoldingsPackageExportFormat.class.getDeclaredFields());
  }

  private List<String> convertFields(Field[] fields) {
    return Arrays.stream(fields)
      .map(Field::getName)
      .collect(Collectors.toList());
  }
}
