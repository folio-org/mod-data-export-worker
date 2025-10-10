package org.folio.dew.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.folio.dew.BaseBatchTest;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.tenant.domain.dto.TenantAttributes;

class TenantControllerTest extends BaseBatchTest {

  @BeforeAll
  static void beforeAll() {
    setUpTenant(NON_CONSORTIUM_TENANT);
  }

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
}
