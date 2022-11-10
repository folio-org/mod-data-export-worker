package org.folio.dew.service;

import org.apache.commons.lang3.StringUtils;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import org.folio.dew.client.ElectronicAccessRelationshipClient;
import org.folio.dew.domain.dto.ElectronicAccessRelationship;
import org.folio.dew.domain.dto.ElectronicAccessRelationshipCollection;
import org.folio.dew.error.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.folio.dew.utils.Constants.ELECTRONIC_RELATIONSHIP_NAME_ID_DELIMITER;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
  void getRelationshipNameAndIdByIdTest() {
    var id = UUID.randomUUID().toString();
    var electronicAccessRelationship = new ElectronicAccessRelationship();
    electronicAccessRelationship.setId(id);
    electronicAccessRelationship.setName("name");

    when(relationshipClient.getById(id)).thenReturn(electronicAccessRelationship);

    var expected = electronicAccessRelationship.getName() + ELECTRONIC_RELATIONSHIP_NAME_ID_DELIMITER + electronicAccessRelationship.getId();
    var actual = electronicAccessService.getRelationshipNameAndIdById(id);

    verify(relationshipClient).getById(id);
    assertEquals(expected, actual);
  }

  @Test
  void getRelationshipNameAndIdByIdNotFoundExceptionTest() {
    var id = UUID.randomUUID().toString();
    var electronicAccessRelationship = new ElectronicAccessRelationship();
    electronicAccessRelationship.setId(id);
    electronicAccessRelationship.setName("name");

    when(relationshipClient.getById(id)).thenThrow(new NotFoundException("error message"));

    var expected = EMPTY +  ELECTRONIC_RELATIONSHIP_NAME_ID_DELIMITER + electronicAccessRelationship.getId();
    var actual = electronicAccessService.getRelationshipNameAndIdById(id);

    assertEquals(expected, actual);
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
