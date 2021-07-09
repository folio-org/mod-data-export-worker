package org.folio.dew;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.batch.test.AssertFile.assertFileEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.folio.des.domain.JobParameterNames;
import org.folio.des.domain.dto.BursarFeeFines;
import org.folio.des.domain.dto.BursarFeeFinesTypeMapping;
import org.folio.des.domain.dto.BursarFeeFinesTypeMapping.ItemCodeEnum;
import org.folio.des.domain.dto.ExportType;
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

class BursarFeesFinesTest extends BaseBatchTest {

  private static final String USERS_GET_REQUEST =
      "/users?query=%28active%3D%3D%22true%22%20and%20patronGroup%3D%3D%283684a786-6671-4268-8ed0-9db82ebca60b%29%29&limit=10000";
  private static final String FEEFINEACTIONS_GET_REQUEST =
      "/feefineactions?query=%28accountId%3D%3D%28807becbc-c3e6-4871-bf38-d140597e41cb%20or%20707becbc-c3e6-4871-bf38-d140597e41cb%20or%20907becbc-c3e6-4871-bf38-d140597e41cb%29%20and%20%28typeAction%3D%3D%28%22Refunded%20partially%22%20or%20%22Refunded%20fully%22%29%29%29&limit=10000";

  private static final String ACCOUNTS_GET_REQUEST = "/accounts";
  private static final String TRANSFERS_GET_REQUEST =
      "/transfers?query=id%3D%3D998ecb15-9f5d-4674-b288-faad24e44c0b&limit=1";
  private static final String SERVICE_POINTS_GET_REQUEST =
      "/service-points?query=code%3D%3Dsystem&limit=2";
  private static final String PATRON_GROUP_WITH_USERS = "3684a786-6671-4268-8ed0-9db82ebca60b";
  private static final String PATRON_GROUP_WITH_NO_USER = "0004a786-6671-4268-8ed0-9db82ebca600";
  private static final String EXPECTED_CHARGE_OUTPUT =
      "src/test/resources/output/charge_bursar_export.dat";
  private static final String EXPECTED_REFUND_OUTPUT =
      "src/test/resources/output/refund_bursar_export.dat";
  @Autowired private Job bursarExportJob;

  @Test
  @DisplayName("BursarExportJob run successfully and passed all steps")
  void successfulBursarExport() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(bursarExportJob);

    final JobParameters jobParameters = prepareJobParameters(PATRON_GROUP_WITH_USERS);
    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    verifyFileOutput(jobExecution);
    verifyServerCalls();
  }

  private void verifyFileOutput(JobExecution jobExecution) throws Exception {
    final ExecutionContext executionContext = jobExecution.getExecutionContext();
    final String filesInStorage = (String) executionContext.get("outputFilesInStorage");
    final String[] split = filesInStorage.split(";");

    final FileSystemResource actualChargeFeesFinesOutput = actualFileOutput(split[0]);
    FileSystemResource expectedCharges = new FileSystemResource(EXPECTED_CHARGE_OUTPUT);
    assertFileEquals(expectedCharges, actualChargeFeesFinesOutput);

    final FileSystemResource actualRefundFeesFinesOutput = actualFileOutput(split[1]);
    FileSystemResource expectedRefund = new FileSystemResource(EXPECTED_REFUND_OUTPUT);
    assertFileEquals(expectedRefund, actualRefundFeesFinesOutput);
  }

  private void verifyServerCalls() {
    wireMockServer.verify(getRequestedFor(urlEqualTo(USERS_GET_REQUEST)));
    wireMockServer.verify(getRequestedFor(urlPathMatching(ACCOUNTS_GET_REQUEST)));
    wireMockServer.verify(getRequestedFor(urlEqualTo(FEEFINEACTIONS_GET_REQUEST)));
    wireMockServer.verify(getRequestedFor(urlEqualTo(TRANSFERS_GET_REQUEST)));
    wireMockServer.verify(getRequestedFor(urlEqualTo(SERVICE_POINTS_GET_REQUEST)));
    wireMockServer.verify(
        postRequestedFor(urlEqualTo("/accounts-bulk/transfer"))
            .withRequestBody(matchingJsonPath("$.amount", equalTo("900.0")))
            .withRequestBody(
                matchingJsonPath(
                    "$.servicePointId", equalTo("afdb59ae-1185-4cd7-94dd-39a87fe01c51")))
            .withRequestBody(matchingJsonPath("$.paymentMethod", equalTo("Transfer2bursar")))
            .withRequestBody(matchingJsonPath("$.notifyPatron", equalTo("false")))
            .withRequestBody(matchingJsonPath("$.userName", equalTo("System")))
            .withRequestBody(
                matchingJsonPath(
                    "$.accountIds",
                    equalTo(
                        "[ \"807becbc-c3e6-4871-bf38-d140597e41cb\", \"707becbc-c3e6-4871-bf38-d140597e41cb\", \"907becbc-c3e6-4871-bf38-d140597e41cb\" ]"))));
  }

  @Test
  @DisplayName("No users for export")
  void noUsersForBursarExport() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(bursarExportJob);

    final JobParameters jobParameters = prepareJobParameters(PATRON_GROUP_WITH_NO_USER);
    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.FAILED);
  }

  private JobParameters prepareJobParameters(String patronGroup) throws JsonProcessingException {
    BursarFeeFines feeFines = new BursarFeeFines();
    feeFines.setPatronGroups(List.of(patronGroup));
    feeFines.setDaysOutstanding(2);
    feeFines.setTransferAccountId(UUID.fromString("998ecb15-9f5d-4674-b288-faad24e44c0b"));

    BursarFeeFinesTypeMapping typeMapping = new BursarFeeFinesTypeMapping();
    typeMapping.setFeefineTypeId(UUID.randomUUID());
    typeMapping.setItemType("itemType");
    typeMapping.setItemCode(ItemCodeEnum.PAYMENT);
    typeMapping.setItemDescription("Desc");

    BursarFeeFinesTypeMapping typeMapping2 = new BursarFeeFinesTypeMapping();
    typeMapping2.setFeefineTypeId(UUID.fromString("933336fd-0290-468a-b69f-35815b713265"));
    typeMapping2.setItemType("Bursar Type");
    typeMapping2.setItemCode(ItemCodeEnum.CHARGE);
    typeMapping2.setItemDescription("Desc bursar                  1");

    final Map<String, List<BursarFeeFinesTypeMapping>> mapping = new HashMap<>();
    mapping.put("ownerId", List.of(typeMapping));
    mapping.put("782c9784-cba0-480a-b8c0-1ffba088c9a4", List.of(typeMapping2));

    feeFines.setTypeMappings(mapping);

    Map<String, JobParameter> params = new HashMap<>();
    params.put("bursarFeeFines", new JobParameter(objectMapper.writeValueAsString(feeFines)));

    String jobId = UUID.randomUUID().toString();
    params.put(JobParameterNames.JOB_ID, new JobParameter(jobId));

    Date now = new Date();
    String workDir =
        System.getProperty("java.io.tmpdir")
            + File.separator
            + springApplicationName
            + File.separator;
    final String outputFile =
        String.format(
            "%s%s_%tF_%tH%tM%tS_%s",
            workDir, ExportType.BURSAR_FEES_FINES, now, now, now, now, jobId);
    params.put(JobParameterNames.TEMP_OUTPUT_FILE_PATH, new JobParameter(outputFile));

    return new JobParameters(params);
  }
}
