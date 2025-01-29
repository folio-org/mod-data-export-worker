package org.folio.dew.service;

import org.folio.dew.BaseBatchTest;
import org.folio.dew.client.DepartmentClient;
import org.folio.dew.client.GroupClient;
import org.folio.dew.domain.dto.Department;
import org.folio.dew.domain.dto.ErrorServiceArgs;
import org.folio.dew.domain.dto.ErrorType;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.error.NotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;


class UserReferenceServiceTest extends BaseBatchTest {

  @MockBean
  private DepartmentClient departmentClient;
  @MockBean
  private GroupClient groupClient;
  @MockBean
  private BulkEditProcessingErrorsService errorService;
  @Autowired
  private UserReferenceService userReferenceService;

  @Test
  void getDepartmentByIdTest() {
    when(departmentClient.getDepartmentById("id")).thenReturn(new Department());

    userReferenceService.getDepartmentNameById("id", new ErrorServiceArgs("jobId", "ïdentifier", "fileName"));

    verify(departmentClient).getDepartmentById("id");
  }

  @Test
  void getPatronGroupNameByIdTest() {
    doThrow(NotFoundException.class).when(groupClient).getGroupById("id");

    var id = userReferenceService.getPatronGroupNameById("id", new ErrorServiceArgs("jobId", "identifier", "fileName"));

    verify(errorService, times(1))
      .saveErrorInCSV(eq("jobId"), eq("identifier"), any(BulkEditException.class), eq("fileName"));

    assertEquals("id", id);
  }

}
