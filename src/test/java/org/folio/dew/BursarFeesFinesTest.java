package org.folio.dew;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.folio.dew.batch.bursarfeesfines.service.BursarExportService;
import org.folio.dew.domain.dto.BursarExportDataToken;
import org.folio.dew.domain.dto.BursarExportFilterPass;
import org.folio.dew.domain.dto.BursarExportJob;
import org.folio.dew.domain.dto.BursarExportTokenFeeMetadata;
import org.folio.dew.domain.dto.BursarExportTransferCriteria;
import org.folio.dew.domain.dto.BursarExportTransferCriteriaConditionsInner;
import org.folio.dew.domain.dto.BursarExportTransferCriteriaElse;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.domain.dto.JobParameterNames;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;

class BursarFeesFinesTest extends BaseBatchTest {

  private static final String USERS_GET_REQUEST =
    "/users?query=%28active%3D%3D%22true%22%20and%20patronGroup%3D%3D%283684a786-6671-4268-8ed0-9db82ebca60b%29%29&limit=10000";
  private static final String ITEMS_GET_REQUEST = "/inventory/items";
  private static final String FEEFINEACTIONS_GET_REQUEST =
    "/feefineactions?query=%28accountId%3D%3D%28807becbc-c3e6-4871-bf38-d140597e41cb%20or%20707becbc-c3e6-4871-bf38-d140597e41cb%20or%20907becbc-c3e6-4871-bf38-d140597e41cb%29%20and%20%28typeAction%3D%3D%28%22Refunded%20partially%22%20or%20%22Refunded%20fully%22%29%29%29&limit=10000";

  private static final String ACCOUNTS_GET_REQUEST = "/accounts";
  private static final String TRANSFERS_GET_REQUEST =
    "/transfers?query=id%3D%3D998ecb15-9f5d-4674-b288-faad24e44c0b&limit=1";
  private static final String SERVICE_POINTS_GET_REQUEST =
    "/service-points?query=code%3D%3Dsystem&limit=2";
  private static final String PATRON_GROUP_WITH_USERS =
    "3684a786-6671-4268-8ed0-9db82ebca60b";
  private static final String PATRON_GROUP_WITH_NO_USER =
    "0004a786-6671-4268-8ed0-9db82ebca600";
  private static final String EXPECTED_CHARGE_OUTPUT =
    "src/test/resources/output/charge_bursar_export.dat";
  private static final String EXPECTED_REFUND_OUTPUT =
    "src/test/resources/output/refund_bursar_export.dat";

  @Autowired
  private Job bursarExportJob;

  @Autowired
  private BursarExportService bursarExportService;

  @Test
  @DisplayName("Run bursar export job with no fees/fines created")
  void testNoFeesFines() throws Exception {
    // stub GET accounts endpoint to return no accounts
    wireMockServer.stubFor(
      get(urlEqualTo("/accounts"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(
              "{\n" +
              "  \"accounts\": [],\n" +
              "  \"totalRecords\": 0,\n" +
              "  \"resultInfo\": {\n" +
              "    \"totalRecords\": 0,\n" +
              "    \"facets\": [],\n" +
              "    \"diagnostics\": []\n" +
              "  }\n" +
              "}"
            )
        )
    );

    JobLauncherTestUtils testLauncher = createTestLauncher(bursarExportJob);

    final JobParameters jobParameters = prepareNoFeesFinesJobParameters();
    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    // job status should be FAILED
    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.FAILED);

    // assert that the ACCOUNT_GET_REQUEST endpoint was hit
    wireMockServer.verify(
      getRequestedFor(urlPathMatching(ACCOUNTS_GET_REQUEST))
    );
    // todo: assert that no new file was created

  }

  //  private void verifyServerCallsNoFeesFines() {
  //    // todo: i just copied this from the original MDEW
  //    wireMockServer.verify(getRequestedFor(urlPathMatching(ACCOUNTS_GET_REQUEST)));
  //    wireMockServer.verify(getRequestedFor(urlEqualTo(USERS_GET_REQUEST)));
  //    wireMockServer.verify(getRequestedFor(urlEqualTo(ITEMS_GET_REQUEST)));
  //    wireMockServer.verify(getRequestedFor(urlEqualTo(FEEFINEACTIONS_GET_REQUEST)));
  //    wireMockServer.verify(getRequestedFor(urlEqualTo(TRANSFERS_GET_REQUEST)));
  //    wireMockServer.verify(getRequestedFor(urlEqualTo(SERVICE_POINTS_GET_REQUEST)));
  //    wireMockServer.verify(
  //      postRequestedFor(urlEqualTo("/accounts-bulk/transfer"))
  //        .withRequestBody(matchingJsonPath("$.amount", equalTo("900.0")))
  //        .withRequestBody(
  //          matchingJsonPath(
  //            "$.servicePointId", equalTo("afdb59ae-1185-4cd7-94dd-39a87fe01c51")))
  //        .withRequestBody(matchingJsonPath("$.paymentMethod", equalTo("Transfer2bursar")))
  //        .withRequestBody(matchingJsonPath("$.notifyPatron", equalTo("false")))
  //        .withRequestBody(matchingJsonPath("$.userName", equalTo("System")))
  //        .withRequestBody(
  //          matchingJsonPath(
  //            "$.accountIds",
  //            equalTo(
  //              "[ \"807becbc-c3e6-4871-bf38-d140597e41cb\", \"707becbc-c3e6-4871-bf38-d140597e41cb\", \"907becbc-c3e6-4871-bf38-d140597e41cb\" ]"))));
  //  }

  private JobParameters prepareNoFeesFinesJobParameters()
    throws JsonProcessingException {
    BursarExportJob job = new BursarExportJob();
    List<BursarExportDataToken> dataTokens = new ArrayList<>();

    BursarExportFilterPass filterPass = new BursarExportFilterPass();

    BursarExportTokenFeeMetadata tokenFeeMetadata = new BursarExportTokenFeeMetadata();
    tokenFeeMetadata.setValue(BursarExportTokenFeeMetadata.ValueEnum.ID);
    dataTokens.add(tokenFeeMetadata);

    job.setData(dataTokens);
    job.setFilter(filterPass);
    job.setGroupByPatron(false);

    BursarExportTransferCriteria transferCriteria = new BursarExportTransferCriteria();

    List<BursarExportTransferCriteriaConditionsInner> transferConditions = new ArrayList<>();

    BursarExportTransferCriteriaElse transferInfo = new BursarExportTransferCriteriaElse();
    transferInfo.setAccount(
      UUID.fromString("998ecb15-9f5d-4674-b288-faad24e44c0b")
    );

    transferCriteria.setConditions(transferConditions);
    transferCriteria.setElse(transferInfo);

    job.setTransferInfo(transferCriteria);

    var parametersBuilder = new JobParametersBuilder();
    parametersBuilder.addString(
      "bursarFeeFines",
      objectMapper.writeValueAsString(job)
    );

    String jobId = UUID.randomUUID().toString();
    parametersBuilder.addString(JobParameterNames.JOB_ID, jobId);

    Date now = new Date();
    String workDir =
      System.getProperty("java.io.tmpdir") +
      File.separator +
      springApplicationName +
      File.separator;
    final String outputFile = String.format(
      "%s%s_%tF_%tH%tM%tS_%s",
      workDir,
      ExportType.BURSAR_FEES_FINES,
      now,
      now,
      now,
      now,
      jobId
    );
    parametersBuilder.addString(
      JobParameterNames.TEMP_OUTPUT_FILE_PATH,
      outputFile
    );

    return parametersBuilder.toJobParameters();
  }
}
