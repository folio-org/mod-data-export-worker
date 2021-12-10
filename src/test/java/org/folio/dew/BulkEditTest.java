package org.folio.dew;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.folio.dew.domain.dto.Address;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.JobParameterNames;
import org.folio.dew.domain.dto.Personal;
import org.folio.dew.domain.dto.User;
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

import lombok.SneakyThrows;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dew.utils.Constants.FILE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.batch.test.AssertFile.assertFileEquals;

class BulkEditTest extends BaseBatchTest {

  private static final String BARCODES_CSV = "src/test/resources/upload/barcodes.csv";
  private static final String USER_RECORD_CSV = "src/test/resources/upload/bulk_edit_user_record.csv";
  private static final String BARCODES_SOME_NOT_FOUND = "src/test/resources/upload/barcodesSomeNotFound.csv";
  private static final String QUERY_FILE_PATH = "src/test/resources/upload/users_by_group.cql";
  private static final String EXPECTED_BULK_EDIT_OUTPUT = "src/test/resources/output/bulk_edit_identifiers_output.csv";
  private final static String EXPECTED_BULK_EDIT_OUTPUT_SOME_NOT_FOUND = "src/test/resources/output/bulk_edit_identifiers_output_some_not_found.csv";
  private final static String EXPECTED_BULK_EDIT_OUTPUT_ERRORS = "src/test/resources/output/bulk_edit_identifiers_errors_output.csv";

  @Autowired private Job bulkEditJob;
  @Autowired private Job bulkEditCqlJob;
  @Autowired private Job bulkEditUpdateUserRecordsJob;

  @Test
  @DisplayName("Run bulk-edit (identifiers) successfully")
  void uploadIdentifiersJobTest() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(bulkEditJob);

    final JobParameters jobParameters = prepareJobParameters(ExportType.BULK_EDIT_IDENTIFIERS, BARCODES_CSV, true);
    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    verifyFileOutput(jobExecution, EXPECTED_BULK_EDIT_OUTPUT);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

    // check if caching works
    wireMockServer.verify(1, getRequestedFor(urlEqualTo("/groups/3684a786-6671-4268-8ed0-9db82ebca60b")));
  }

  @Test
  @DisplayName("Run bulk-edit (identifiers) with errors")
  void bulkEditJobTestWithErrors() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(bulkEditJob);

    final JobParameters jobParameters = prepareJobParameters(ExportType.BULK_EDIT_IDENTIFIERS, BARCODES_SOME_NOT_FOUND, true);
    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    verifyFileOutput(jobExecution, EXPECTED_BULK_EDIT_OUTPUT_SOME_NOT_FOUND);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

    // check if caching works
    wireMockServer.verify(1, getRequestedFor(urlEqualTo("/groups/3684a786-6671-4268-8ed0-9db82ebca60b")));
  }

  @Test
  @DisplayName("Run bulk-edit (query) successfully")
  void bulkEditQueryJobTest() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(bulkEditCqlJob);

    final JobParameters jobParameters = prepareJobParameters(ExportType.BULK_EDIT_QUERY, QUERY_FILE_PATH, true);
    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    verifyFileOutput(jobExecution, EXPECTED_BULK_EDIT_OUTPUT);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
  }

    @Test
    @DisplayName("Run bulk-edit (update user record) successfully")
    void uploadUserRecordsJobTest() throws Exception {
      JobLauncherTestUtils testLauncher = createTestLauncher(bulkEditUpdateUserRecordsJob);
      //    ArgumentCaptor<User> userArgumentCaptor = createUserCaptor();
//
    final JobParameters jobParameters = prepareJobParameters(ExportType.BULK_EDIT_UPDATE, USER_RECORD_CSV, false);
    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
      //    User user = userArgumentCaptor.getValue();
      //    verifyUpdatedUser(user);
//  }
    }

    private void verifyUpdatedUser(User user) {
      assertEquals("User name", user.getUsername());
      assertEquals("User id", user.getId());
      assertEquals("External system id", user.getExternalSystemId());
      assertEquals("Barcode", user.getBarcode());
      assertTrue(user.getActive());
      assertEquals("Type", user.getType());
      assertEquals("PatronGroup", user.getPatronGroup());
      assertEquals("Departments", user.getDepartments().iterator().next());
      assertEquals("2021-12-19 03:23:37.989Z", user.getEnrollmentDate().toString());
      assertEquals("2021-12-20 03:23:37.989Z", user.getExpirationDate().toString());
      assertEquals("2021-12-05 02:23:37.989Z", user.getCreatedDate().toString());
      assertEquals("2021-12-05 03:23:37.989Z", user.getUpdatedDate().toString());
      assertEquals("Tag1", user.getTags().getTagList().iterator().next());

      Map<String, Object> customFields = user.getCustomFields();
      Map.Entry<String, Object> customField = customFields.entrySet().iterator().next();
      assertEquals("Custom", customField.getKey());
      assertEquals("field", customField.getValue().toString());

      Personal personal = user.getPersonal();
      assertEquals("Last name", personal.getLastName());
      assertEquals("First name", personal.getFirstName());
      assertEquals("Middle name", personal.getMiddleName());
      assertEquals("Preferred first name", personal.getPreferredFirstName());
      assertEquals("Email", personal.getEmail());
      assertEquals("Phone", personal.getPhone());
      assertEquals("Mobile phone", personal.getMobilePhone());
      assertEquals("1998-12-19 03:23:37.989Z", personal.getDateOfBirth().toString());
      assertEquals("preferredContactTypeId", personal.getPreferredContactTypeId());

      Address address = personal.getAddresses().iterator().next();
      assertEquals("addressId", address.getId());
      assertEquals("BE", address.getCountryId());
      assertEquals("Address line 1", address.getAddressLine1());
      assertEquals("Address line 2", address.getAddressLine2());
      assertEquals("Some City", address.getCity());
      assertEquals("Some Region", address.getRegion());
      assertEquals("12345", address.getPostalCode());
      assertTrue(address.getPrimaryAddress());
      assertEquals("HomeAddress", address.getAddressTypeId());
    }

  @SneakyThrows
  private void verifyFileOutput(JobExecution jobExecution, String output) {
    final ExecutionContext executionContext = jobExecution.getExecutionContext();
    String fileInStorage = (String) executionContext.get("outputFilesInStorage");
    if (fileInStorage.contains(";")) {
      String[] links = fileInStorage.split(";");
      fileInStorage = links[0];
      String errorInStorage = links[1];
      System.out.println("output: " + output);
      final FileSystemResource actualResultWithErrors = actualFileOutput(errorInStorage);
      final FileSystemResource expectedResultWithErrors =  new FileSystemResource(EXPECTED_BULK_EDIT_OUTPUT_ERRORS);
      assertFileEquals(expectedResultWithErrors, actualResultWithErrors);
    }
    final FileSystemResource actualResult = actualFileOutput(fileInStorage);
    FileSystemResource expectedCharges = new FileSystemResource(output);
    assertFileEquals(expectedCharges, actualResult);
  }

  private JobParameters prepareJobParameters(ExportType exportType, String path, boolean hasOutcomeFile) {
    Map<String, JobParameter> params = new HashMap<>();
    params.put(FILE_NAME, new JobParameter(path));

    if (hasOutcomeFile) {
      String workDir =
        System.getProperty("java.io.tmpdir")
          + File.separator
          + springApplicationName
          + File.separator;
      params.put(JobParameterNames.TEMP_OUTPUT_FILE_PATH, new JobParameter(workDir + "out"));
    }

    if (ExportType.BULK_EDIT_QUERY.equals(exportType)) {
      params.put("query", new JobParameter(readQueryString(path)));
    }

    String jobId = UUID.randomUUID().toString();
    params.put(JobParameterNames.JOB_ID, new JobParameter(jobId));

    return new JobParameters(params);
  }

  @SneakyThrows
  private String readQueryString(String path) {
    try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
      return reader.readLine();
    }
  }

}
