package org.folio.dew.service;

import static org.folio.dew.utils.Constants.EUREKA_PLATFORM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.folio.dew.client.OkapiClient;
import org.folio.dew.error.NotFoundException;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ModUsersModuleServiceTest {

  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private OkapiClient okapiClient;

  @InjectMocks
  private ModUsersModuleService modUsersModuleService;

  @Test
  void getModUsersModuleIdForEurekaTest() {
    var moduleId = "usersModuleId";
    modUsersModuleService.setPlatform(EUREKA_PLATFORM);

    assertThrows(NotFoundException.class, () -> modUsersModuleService.getModUsersModuleId());

    modUsersModuleService.setModUsersId(moduleId);
    var actual = modUsersModuleService.getModUsersModuleId();
    assertEquals(moduleId, actual);
  }
}
