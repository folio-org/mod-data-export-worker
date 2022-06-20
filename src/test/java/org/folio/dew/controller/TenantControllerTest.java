package org.folio.dew.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.folio.dew.utils.Constants.MODULE_NAME;
import static org.folio.dew.utils.Constants.STATUSES_CONFIG_NAME;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import lombok.SneakyThrows;
import org.folio.dew.BaseBatchTest;
import org.folio.dew.domain.dto.ModelConfiguration;
import org.folio.dew.service.BulkEditConfigurationService;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.api.Test;

import java.util.List;

class TenantControllerTest extends BaseBatchTest {

  @Test
  @SneakyThrows
  void canDisableTenantTest() {
    var headers = defaultHeaders();
    headers.put(XOkapiHeaders.TENANT, List.of("test_tenant"));

    mockMvc.perform(post("/_/tenant").content(asJsonString(new TenantAttributes().moduleTo("mod-data-export-worker")))
      .headers(headers)
      .contentType(APPLICATION_JSON)).andExpect(status().isNoContent());

    mockMvc.perform(post("/_/tenant").content(asJsonString(new TenantAttributes().purge(true)))
      .headers(headers)
      .contentType(APPLICATION_JSON)).andExpect(status().isNoContent());
  }

  @Test
  @SneakyThrows
  void shouldPostDefaultStatusesConfigurationUponPostTenantIfNotPresent() {
    var expectedConfiguration = new ModelConfiguration()
      .module(MODULE_NAME)
      .configName(STATUSES_CONFIG_NAME)
      ._default(true)
      .enabled(true)
      .value(objectMapper.writeValueAsString(BulkEditConfigurationService.getAllowedStatuses()));

    wireMockServer.verify(
      postRequestedFor(
        urlEqualTo("/configurations/entries"))
        .withRequestBody(equalToJson(objectMapper.writeValueAsString(expectedConfiguration))));
  }
}
