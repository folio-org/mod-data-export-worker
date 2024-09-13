package org.folio.dew.controller;

import lombok.SneakyThrows;
import org.folio.dew.BaseBatchTest;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PermissionsSelfCheckControllerTest extends BaseBatchTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  @SneakyThrows
  void shouldReturnDesiredPermissions() {
    var headers = defaultHeaders();
    headers.put(XOkapiHeaders.PERMISSIONS, List.of("[\"desired-permission\",\"desired-permission-2\"]"));
    var result = mockMvc.perform(get("/permissions-self-check")
        .headers(headers)).andExpect(status().isOk()).andReturn();

    assertEquals("[\"desired-permission\",\"desired-permission-2\"]", result.getResponse().getContentAsString());
  }
}
