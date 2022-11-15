package org.folio.dew.service;

import org.apache.commons.lang3.StringUtils;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import org.folio.dew.client.ElectronicAccessRelationshipClient;
import org.folio.dew.domain.dto.ElectronicAccess;
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

import static org.folio.dew.utils.Constants.ELECTRONIC_RELATIONSHIP_NAME_ID_DELIMITER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ElectronicAccessServiceTest {

  @Mock
  private ElectronicAccessRelationshipClient relationshipClient;
  @Mock
  private BulkEditProcessingErrorsService bulkEditProcessingErrorsService;

  @InjectMocks
  private ElectronicAccessService electronicAccessService;

  @Test
  void electronicAccessesToStringTest() {
    var relationshipId = "relationshipId";
    var electronicAccess = new ElectronicAccess();
    electronicAccess.setRelationshipId(relationshipId);
    electronicAccess.setUri("uri");

    var electronicAccessRelationship = new ElectronicAccessRelationship();
    electronicAccessRelationship.setId(relationshipId);
    electronicAccessRelationship.setName("name");

    when(relationshipClient.getById(relationshipId)).thenReturn(electronicAccessRelationship);

    var expected = "uri;;;;name;relationshipId";
    var actual = electronicAccessService.getElectronicAccessesToString(List.of(electronicAccess), "formatIdentifier", "jobId", "fileName");

    assertEquals(expected, actual);
  }

  @Test
  void getElectronicAccessesToStringElectronicAccessRelationshipNotFoundByIdTest() {
    var relationshipId = "relationshipId";
    var electronicAccess = new ElectronicAccess();
    electronicAccess.setRelationshipId(relationshipId);
    electronicAccess.setUri("uri");

    when(relationshipClient.getById(relationshipId)).thenThrow(new NotFoundException("error message"));

    var expected = "uri;;;;;relationshipId";
    var actual = electronicAccessService.getElectronicAccessesToString(List.of(electronicAccess),
      "formatIdentifier", "jobId", "fileName");

    verify(bulkEditProcessingErrorsService).saveErrorInCSV(isA(String.class), isA(String.class), isA(BulkEditException.class), isA(String.class));
    assertEquals(expected, actual);
  }

  @Test
  void getElectronicAccessesToStringElectronicAccessRelationshipIsNullTest() {
    var electronicAccess = new ElectronicAccess();
    electronicAccess.setUri("uri");

    var expected = "uri;;;;;";
    var actual = electronicAccessService.getElectronicAccessesToString(List.of(electronicAccess),
      "formatIdentifier", "jobId", "fileName");

    assertEquals(expected, actual);
  }

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
