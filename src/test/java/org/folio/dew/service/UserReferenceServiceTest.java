package org.folio.dew.service;

import org.folio.dew.client.AddressTypeClient;
import org.folio.dew.client.DepartmentClient;
import org.folio.dew.domain.dto.AddressTypeCollection;
import org.folio.dew.domain.dto.Department;
import org.folio.dew.domain.dto.DepartmentCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserReferenceServiceTest {

  @Mock
  private AddressTypeClient addressTypeClient;
  @Mock
  private DepartmentClient departmentClient;

  @InjectMocks
  private UserReferenceService userReferenceService;

  @Test
  void getAddressTypeByDescTest() {
    when(addressTypeClient.getAddressTypeByQuery("desc=\"name\"")).thenReturn(new AddressTypeCollection());

    userReferenceService.getAddressTypeIdByDesc("name");

    verify(addressTypeClient).getAddressTypeByQuery("desc=\"name\"");
  }

  @Test
  void getDepartmentByIdTest() {
    when(departmentClient.getDepartmentById("id")).thenReturn(new Department());

    userReferenceService.getDepartmentIdByName("id");

    verify(departmentClient).getDepartmentById("id");
  }

  @Test
  void getDepartmentByNameTest() {
    when(departmentClient.getDepartmentByQuery("name=\"name\"")).thenReturn(new DepartmentCollection());

    userReferenceService.getDepartmentIdByName("name");

    verify(departmentClient).getDepartmentByQuery("name=\"name\"");
  }

}
