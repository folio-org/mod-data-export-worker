package org.folio.dew;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

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
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;

class BursarFeesFinesTest extends BaseBatchTest {

  private static final String USERS_ENDPOINT_PATH = "/users";
  private static final String ITEMS_ENDPOINT_PATH = "/inventory/items";

  private static final String ALL_OPEN_ACCOUNTS_GET_REQUEST =
    "/accounts?query=remaining%20%3E%200.0&limit=10000";
  private static final String TRANSFERS_ENDPOINT_PATH = "/transfers";

  private static final String SERVICE_POINTS_GET_REQUEST =
    "/service-points?query=code%3D%3Dsystem&limit=2";

  @Autowired
  private Job bursarExportJob;

  @Autowired
  private BursarExportService bursarExportService;

  @Test
  @DisplayName("Run bursar export job with no fees/fines created")
  void testNoFeesFines() throws Exception {
    // stub GET accounts endpoint to return no accounts
    wireMockServer.stubFor(
      get(urlEqualTo(ALL_OPEN_ACCOUNTS_GET_REQUEST))
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
    assertThat(jobExecution.getExitStatus(), is(ExitStatus.FAILED));

    // check that the ACCOUNT_GET_REQUEST endpoint was hit
    wireMockServer.verify(
      getRequestedFor(urlEqualTo(ALL_OPEN_ACCOUNTS_GET_REQUEST))
    );

    // check that user and items endpoints were not hit
    wireMockServer.verify(
      0,
      getRequestedFor(urlPathMatching(USERS_ENDPOINT_PATH))
    );
    wireMockServer.verify(
      0,
      getRequestedFor(urlPathMatching(ITEMS_ENDPOINT_PATH))
    );
    wireMockServer.verify(
      0,
      postRequestedFor(urlPathMatching(TRANSFERS_ENDPOINT_PATH))
    );
    wireMockServer.verify(
      0,
      postRequestedFor(urlEqualTo(SERVICE_POINTS_GET_REQUEST))
    );

    // check that no new file was created
    final ExecutionContext executionContext = jobExecution.getExecutionContext();
    final String filesInStorage = (String) executionContext.get(
      "outputFilesInStorage"
    );

    assertThat(filesInStorage, nullValue());
  }

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
