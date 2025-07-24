package org.folio.dew.bursarfeesfines;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.folio.dew.BaseBatchTest;
import org.folio.dew.error.BursarNoAccountsToTransferException;
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
import org.springframework.core.io.FileSystemResource;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

class NoFeeFineMatchingAggregateCriteriaAggregateTest extends BaseBatchTest {

  @Autowired
  private Job bursarExportJob;

  private static final String GET_USERS_REQUEST = "/users?query=id%3D%3D%28%28bec20636-fb68-41fd-84ea-2cf910673599%29%29&limit=50";

  private static final String GET_ITEMS_REQUEST = "/inventory/items?query=id%3D%3D%28%28100d10bf-2f06-4aa0-be15-0b95b2d9f9e4%20or%20100d10bf-2f06-4aa0-be15-0b95b2d9f9e3%29%29&limit=50";

  private static final String EXPECTED_ALL_OPEN_ACCOUNTS_JSON_OUTPUT = "src/test/resources/output/bursar_all_open_accounts_json_output.json";

  @Test
  @DisplayName("Run bursar export that has fee/fines matching initial criteria but not aggregate criteria")
  void testNoFeeFineMatchingAggregateCriteria() throws Exception {
    // stub request to get accounts
    wireMockServer
      .stubFor(get(urlEqualTo(BursarFeesFinesTestUtils.ALL_OPEN_ACCOUNTS_GET_REQUEST)).willReturn(aResponse().withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBody(FileUtils.readFileToString(new FileSystemResource(EXPECTED_ALL_OPEN_ACCOUNTS_JSON_OUTPUT).getFile(), "UTF-8"))));

    // stub request to get users
    wireMockServer.stubFor(get(urlEqualTo(GET_USERS_REQUEST)).willReturn(aResponse().withStatus(200)
      .withHeader("Content-Type", "application/json")
      .withBody("""
          {
            "users": [
              {
                "username": "morty",
                "id": "bec20636-fb68-41fd-84ea-2cf910673599",
                "externalSystemId": "ExSysID",
                "barcode": "456",
                "active": true,
                "patronGroup": "3684a786-6671-4268-8ed0-9db82ebca60b",
                "departments": [],
                "proxyFor": [],
                "personal": {
                  "lastName": "morty",
                  "firstName": "panic",
                  "email": "morty@example.com",
                  "addresses": [],
                  "preferredContactTypeId": "002"
                },
                "enrollmentDate": "2020-10-07T04:00:00.000+00:00",
                "createdDate": "2021-03-26T10:39:58.098+00:00",
                "updatedDate": "2021-03-26T10:39:58.098+00:00",
                "metadata": {
                  "createdDate": "2021-02-25T11:12:26.119+00:00",
                  "updatedDate": "2021-03-26T10:39:57.994+00:00",
                  "updatedByUserId": "61187964-6bb3-526f-bdaa-e20e8e2f9305"
                },
                "customFields": {}
              }
            ],
            "totalRecords": 1,
            "resultInfo": {
              "totalRecords": 1,
              "facets": [],
              "diagnostics": []
            }
          }
          """)));

    // stub request to get items
    wireMockServer.stubFor(get(urlEqualTo(GET_ITEMS_REQUEST)).willReturn(aResponse().withStatus(200)
      .withHeader("Content-Type", "application/json")
      .withBody("""
          {
            "items": [
              {
                "id": "100d10bf-2f06-4aa0-be15-0b95b2d9f9e3",
                "effectiveLocation": {
                  "id": "53cf956f-c1df-410b-8bea-27f712cca7c0",
                  "name": "Annex"
                },
                "materialType": {
                  "id": "1a54b431-2e4f-452d-9cae-9cee66c9a892",
                  "name": "book"
                },
                "barcode": "123123123",
                "title": "item 1"
              },
              {
                "id": "100d10bf-2f06-4aa0-be15-0b95b2d9f9e4",
                "effectiveLocation": {
                  "id": "53cf956f-c1df-410b-8bea-27f712cca7c1",
                  "name": "Annex2"
                },
                "materialType": {
                  "id": "1a54b431-2e4f-452d-9cae-9cee66c9a893",
                  "name": "book2"
                },
                "barcode": "123123124",
                "title": "item 2"
              }
            ],
            "totalRecords": 2,
            "resultInfo": {
              "totalRecords": 2,
              "facets": [],
              "diagnostics": []
            }
          }
          """)));

    // stub request to transfer accounts
    wireMockServer
      .stubFor(post(urlEqualTo(BursarFeesFinesTestUtils.TRANSFER_ACCOUNTS_ENDPOINT)).willReturn(aResponse().withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBody("""
            {
            }""")));

    JobLauncherTestUtils testLauncher = createTestLauncher(bursarExportJob);

    final JobParameters jobParameters = BursarFeesFinesTestUtils
      .prepareNoFeeFineMatchingAggregateCriteriaAggregateTest(springApplicationName, objectMapper);

    JobExecution jobExecution = testLauncher.launchJob(jobParameters);

    assertThat(jobExecution.getExitStatus(), is(ExitStatus.FAILED));
    assertThat(jobExecution.getAllFailureExceptions(), contains(instanceOf(BursarNoAccountsToTransferException.class)));

    wireMockServer.verify(getRequestedFor(urlEqualTo(BursarFeesFinesTestUtils.ALL_OPEN_ACCOUNTS_GET_REQUEST)));

    wireMockServer.verify(getRequestedFor(urlEqualTo(GET_USERS_REQUEST)));
    wireMockServer.verify(getRequestedFor(urlPathMatching(BursarFeesFinesTestUtils.USERS_ENDPOINT_PATH)));

    wireMockServer.verify(getRequestedFor(urlEqualTo(GET_ITEMS_REQUEST)));
    wireMockServer.verify(getRequestedFor(urlPathMatching(BursarFeesFinesTestUtils.ITEMS_ENDPOINT_PATH)));

    wireMockServer.verify(0, getRequestedFor(urlPathMatching(BursarFeesFinesTestUtils.TRANSFERS_ENDPOINT_PATH)));

    wireMockServer.verify(0, postRequestedFor(urlPathMatching(BursarFeesFinesTestUtils.TRANSFER_ACCOUNTS_ENDPOINT)));

    wireMockServer.verify(0, getRequestedFor(urlEqualTo(BursarFeesFinesTestUtils.SERVICE_POINTS_GET_REQUEST)));

    // check file content
    final ExecutionContext executionContext = jobExecution.getExecutionContext();
    final String filesInStorage = (String) executionContext.get("outputFilesInStorage");
    assertThat(filesInStorage, is(nullValue()));
  }
}
