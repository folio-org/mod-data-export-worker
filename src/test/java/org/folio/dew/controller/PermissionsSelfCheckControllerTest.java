package org.folio.dew.controller;

import lombok.SneakyThrows;
import org.folio.dew.BaseBatchTest;
import org.folio.dew.batch.bulkedit.jobs.permissions.check.UserPermissions;
import org.folio.dew.client.UserPermissionsClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PermissionsSelfCheckControllerTest extends BaseBatchTest {

  @MockBean
  private UserPermissionsClient userPermissionsClient;
  @Autowired
  private MockMvc mockMvc;

  @Test
  @SneakyThrows
  void shouldReturnDesiredPermissions() {
    var headers = defaultHeaders();
    when(userPermissionsClient.getPermissions(isA(String.class)))
      .thenReturn(UserPermissions.builder().permissionNames(List.of("permission-1", "permission-2")).build());
    var result = mockMvc.perform(get("/bulk-edit/permissions-self-check")
        .headers(headers)).andExpect(status().isOk()).andReturn();

    assertEquals("[\"permission-1\",\"permission-2\"]", result.getResponse().getContentAsString());
  }
}
