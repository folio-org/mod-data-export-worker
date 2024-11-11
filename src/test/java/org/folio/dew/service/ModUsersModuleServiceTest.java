package org.folio.dew.service;

import static org.folio.dew.utils.Constants.EUREKA_PLATFORM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import org.folio.dew.client.OkapiClient;
import org.folio.dew.error.NotFoundException;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;

@ExtendWith(MockitoExtension.class)
public class ModUsersModuleServiceTest {

  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private OkapiClient okapiClient;

  @InjectMocks
  private ModUsersModuleService modUsersModuleService;

  @Test
  void getModUsersModuleIdForOkapiTest() {
    var tenantId = "diku";
    JsonNode arrayNode = new com.fasterxml.jackson.databind.node.ArrayNode(new
      com.fasterxml.jackson.databind.node.JsonNodeFactory(true));

    when(folioExecutionContext.getTenantId()).thenReturn(tenantId);
    when(okapiClient.getModuleIds(isA(URI.class), eq(tenantId), eq("mod-users"))).thenReturn(arrayNode);

    assertThrows(NotFoundException.class, ()-> modUsersModuleService.getModUsersModuleId());
    verify(okapiClient).getModuleIds(isA(URI.class), eq(tenantId), eq("mod-users"));
  }

  @Test
  void getModUsersModuleIdForEurekaTest() {
    var moduleId = "usersModuleId";
    modUsersModuleService.setPlatform(EUREKA_PLATFORM);

    assertThrows(NotFoundException.class, ()-> modUsersModuleService.getModUsersModuleId());

    modUsersModuleService.setModUsersId(moduleId);
    var actual = modUsersModuleService.getModUsersModuleId();
    assertEquals(moduleId, actual);
  }
}
