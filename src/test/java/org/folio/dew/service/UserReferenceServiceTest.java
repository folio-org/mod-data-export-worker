package org.folio.dew.service;

import org.folio.dew.BaseBatchTest;
import org.folio.dew.client.DepartmentClient;
import org.folio.dew.client.GroupClient;
import org.folio.dew.domain.dto.Department;
import org.folio.dew.domain.dto.ErrorServiceArgs;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.error.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class UserReferenceServiceTest extends BaseBatchTest {

  @MockitoBean
  private DepartmentClient departmentClient;
  @MockitoBean
  private GroupClient groupClient;
  @MockitoBean
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
