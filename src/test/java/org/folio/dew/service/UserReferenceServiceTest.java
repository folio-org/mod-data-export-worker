package org.folio.dew.service;

import org.folio.dew.BaseBatchTest;
import org.folio.dew.client.AddressTypeClient;
import org.folio.dew.client.DepartmentClient;
import org.folio.dew.domain.dto.AddressTypeCollection;
import org.folio.dew.domain.dto.Department;
import org.folio.dew.domain.dto.DepartmentCollection;
import org.folio.dew.domain.dto.ErrorServiceArgs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class UserReferenceServiceTest extends BaseBatchTest {

  @MockBean
  private AddressTypeClient addressTypeClient;
  @MockBean
  private DepartmentClient departmentClient;
  @Autowired
  private UserReferenceService userReferenceService;

  @Test
  void getDepartmentByIdTest() {
    when(departmentClient.getDepartmentById("id")).thenReturn(new Department());

    userReferenceService.getDepartmentNameById("id", new ErrorServiceArgs("jobId", "ïdentifier", "fileName"));

    verify(departmentClient).getDepartmentById("id");
  }

}
