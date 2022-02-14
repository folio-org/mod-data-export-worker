package org.folio.dew.controller;

import lombok.SneakyThrows;
import org.folio.dew.BaseJobTest;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TenantControllerTest extends BaseJobTest {
  @Test
  @SneakyThrows
  void canDeleteTenantTest() {
    var headers = defaultHeaders();
    headers.put(XOkapiHeaders.TENANT, List.of("test_tenant"));

    mockMvc.perform(post("/_/tenant").content(asJsonString(new TenantAttributes().moduleTo("mod-data-export-worker")))
      .headers(headers)
      .contentType(APPLICATION_JSON)).andExpect(status().isNoContent());

    mockMvc.perform(delete("/_/tenant/test_tenant")
      .headers(headers)
      .contentType(APPLICATION_JSON)).andExpect(status().isNoContent());
  }
}
