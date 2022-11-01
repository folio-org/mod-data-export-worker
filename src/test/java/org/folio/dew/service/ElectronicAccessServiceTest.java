package org.folio.dew.service;

import org.apache.commons.lang3.StringUtils;
import org.folio.dew.client.ElectronicAccessRelationshipClient;
import org.folio.dew.domain.dto.ElectronicAccessRelationship;
import org.folio.dew.domain.dto.ElectronicAccessRelationshipCollection;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.error.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ElectronicAccessServiceTest {

  @Mock
  private ElectronicAccessRelationshipClient relationshipClient;

  @InjectMocks
  private ElectronicAccessService electronicAccessService;

  @Test
  void getRelationshipByIdTest() {
    var id = UUID.randomUUID().toString();
    var electronicAccessRelationship = new ElectronicAccessRelationship();
    electronicAccessRelationship.setId(id);

    when(relationshipClient.getById(id)).thenReturn(electronicAccessRelationship);

    var actualElectronicAccessRelationship = electronicAccessService.getRelationshipById(id);

    verify(relationshipClient).getById(id);
    assertEquals(actualElectronicAccessRelationship.getId(), id);
  }

  @Test
  void getRelationshipByIdNotFoundExceptionTest() {
    var id = UUID.randomUUID().toString();
    var electronicAccessRelationship = new ElectronicAccessRelationship();
    electronicAccessRelationship.setId(id);

    when(relationshipClient.getById(id)).thenThrow(new NotFoundException("error message"));

    assertThrows(BulkEditException.class, ()->electronicAccessService.getRelationshipById(id));
  }

  @Test
  void getElectronicAccessRelationshipIdByNameTest() {
    var id = UUID.randomUUID().toString();
    var electronicAccessRelationship = new ElectronicAccessRelationship();
    electronicAccessRelationship.setId(id);
    electronicAccessRelationship.name("name");

    var electronicAccessRelationshipCollection = new ElectronicAccessRelationshipCollection();
    electronicAccessRelationshipCollection.setElectronicAccessRelationships(List.of(electronicAccessRelationship));

    when(relationshipClient.getByQuery(isA(String.class))).thenReturn(electronicAccessRelationshipCollection);

    var actualId =  electronicAccessService.getElectronicAccessRelationshipIdByName("name");
    assertEquals(id, actualId);
  }

  @Test
  void getElectronicAccessRelationshipIdByNameWithEmptyCollectionTest() {
    var electronicAccessRelationshipCollection = new ElectronicAccessRelationshipCollection();
    electronicAccessRelationshipCollection.setElectronicAccessRelationships(new ArrayList<>());

    when(relationshipClient.getByQuery(isA(String.class))).thenReturn(electronicAccessRelationshipCollection);

    var actualId = electronicAccessService.getElectronicAccessRelationshipIdByName("name");
    assertEquals(StringUtils.EMPTY, actualId);
  }
}
