package org.folio.dew.bursarfeesfines;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.folio.dew.BaseBatchTest;
import org.folio.dew.helpers.bursarfeesfines.BursarFeesFinesTestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;

class NoFeesFinesTest extends BaseBatchTest {

  @Autowired
  private Job bursarExportJob;

  @Test
  @DisplayName("Run bursar export job with no fees/fines created")
  void testNoFeesFines() throws Exception {
    // stub GET accounts endpoint to return no accounts
    wireMockServer.stubFor(
      get(urlEqualTo(BursarFeesFinesTestUtils.ALL_OPEN_ACCOUNTS_GET_REQUEST))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
                {
                  "accounts": [],
                  "totalRecords": 0,
                  "resultInfo": {
                    "totalRecords": 0,
                    "facets": [],
                    "diagnostics": []
                  }
                }"""
            )
        )
    );

    JobLauncherTestUtils testLauncher = createTestLauncher(bursarExportJob);

    final JobParameters jobParameters = BursarFeesFinesTestUtils.prepareNoFeesFinesJobParameters(
      springApplicationName,
      objectMapper
    );
    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    // job status should be FAILED
    assertThat(jobExecution.getExitStatus(), is(ExitStatus.FAILED));

    // check that the ACCOUNT_GET_REQUEST endpoint was hit
    wireMockServer.verify(
      getRequestedFor(
        urlEqualTo(BursarFeesFinesTestUtils.ALL_OPEN_ACCOUNTS_GET_REQUEST)
      )
    );

    // check that user and items endpoints were not hit
    wireMockServer.verify(
      0,
      getRequestedFor(
        urlPathMatching(BursarFeesFinesTestUtils.USERS_ENDPOINT_PATH)
      )
    );
    wireMockServer.verify(
      0,
      getRequestedFor(
        urlPathMatching(BursarFeesFinesTestUtils.ITEMS_ENDPOINT_PATH)
      )
    );
    wireMockServer.verify(
      0,
      postRequestedFor(
        urlPathMatching(BursarFeesFinesTestUtils.TRANSFERS_ENDPOINT_PATH)
      )
    );
    wireMockServer.verify(
      0,
      getRequestedFor(
        urlEqualTo(BursarFeesFinesTestUtils.SERVICE_POINTS_GET_REQUEST)
      )
    );

    // check that no new file was created
    final ExecutionContext executionContext = jobExecution.getExecutionContext();
    final String filesInStorage = (String) executionContext.get(
      "outputFilesInStorage"
    );

    assertThat(filesInStorage, nullValue());
  }
}
