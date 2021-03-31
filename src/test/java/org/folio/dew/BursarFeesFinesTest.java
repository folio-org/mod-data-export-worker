package org.folio.dew;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;

class BursarFeesFinesTest extends BaseBatchTest {

  public static final String PATRON_GROUP_WITH_USERS = "3684a786-6671-4268-8ed0-9db82ebca60b";
  public static final String PATRON_GROUP_WITH_NO_USER = "0004a786-6671-4268-8ed0-9db82ebca600";
  @Autowired private Job bursarExportJob;

  @Test
  @DisplayName("BursarExportJob run successfully and passed all steps")
  void successfulBursarExport() throws Exception {
    JobLauncherTestUtils testLauncher = createTestLauncher(bursarExportJob);

    final JobParameters jobParameters = prepareJobParameters(PATRON_GROUP_WITH_USERS);
    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

    wireMockServer.verify(
        getRequestedFor(
            urlEqualTo(
                "/users?query=%28active%3D%3D%22true%22%20and%20patronGroup%3D%3D%283684a786-6671-4268-8ed0-9db82ebca60b%29%29&limit=10000")));
    wireMockServer.verify(
        getRequestedFor(
            urlEqualTo(
                "/accounts?query=userId%3D%3D%28bec20636-fb68-41fd-84ea-2cf910673599%20or%202205005b-ca51-4a04-87fd-938eefa8f6de%20or%20b4cee18d-f862-4ef1-95a5-879fdd619603%29%20and%20remaining%20%3E%200.0%20and%20metadata.createdDate%3E%3D2021-03-29&limit=10000")));
    wireMockServer.verify(
        getRequestedFor(
            urlEqualTo(
                "/feefineactions?query=%28accountId%3D%3D%28807becbc-c3e6-4871-bf38-d140597e41cb%20or%20707becbc-c3e6-4871-bf38-d140597e41cb%20or%20907becbc-c3e6-4871-bf38-d140597e41cb%29%20and%20%28typeAction%3D%3D%28%22Refunded%20partially%22%20or%20%22Refunded%20fully%22%29%29%29&limit=10000")));
    wireMockServer.verify(
        getRequestedFor(
            urlEqualTo("/transfers?query=id%3D%3D998ecb15-9f5d-4674-b288-faad24e44c0b&limit=1")));
    wireMockServer.verify(
        getRequestedFor(urlEqualTo("/service-points?query=code%3D%3Dsystem&limit=2")));
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

    feeFines.setTypeMappings(List.of(typeMapping));

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
