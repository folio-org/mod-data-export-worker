package org.folio.dew.service;

import org.apache.commons.lang3.StringUtils;
import org.folio.dew.client.ElectronicAccessRelationshipClient;
import org.folio.dew.domain.dto.ElectronicAccess;
import org.folio.dew.domain.dto.ElectronicAccessRelationship;
import org.folio.dew.domain.dto.ElectronicAccessRelationshipCollection;
import org.folio.dew.domain.dto.ErrorServiceArgs;
import org.folio.dew.error.NotFoundException;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.folio.dew.domain.dto.EntityType.HOLDINGS_RECORD;
import static org.folio.dew.service.FolioExecutionContextManager.X_OKAPI_TENANT;
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
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Spy
  private SpecialCharacterEscaper escaper;

  @InjectMocks
  private ElectronicAccessService electronicAccessService;

  @Test
  void getElectronicAccessesToStringTest() {
    var relationshipId = "relationshipId";
    var electronicAccess = new ElectronicAccess();
    electronicAccess.setRelationshipId(relationshipId);
    electronicAccess.setUri("uri");

    var electronicAccessRelationship = new ElectronicAccessRelationship();
    electronicAccessRelationship.setId(relationshipId);
    electronicAccessRelationship.setName("name");

    when(relationshipClient.getById(relationshipId)).thenReturn(electronicAccessRelationship);

    var expected = "URL relationship;URI;Link text;Materials specified;URL public note\nname\u001F;uri\u001F;\u001F;\u001F;";
    var actual = electronicAccessService.getElectronicAccessesToString(List.of(electronicAccess), buildErrorServiceArgs(), HOLDINGS_RECORD, "tenant");

    assertEquals(expected, actual);
  }

  @Test
  void getElectronicAccessesToStringElectronicAccessRelationshipNotFoundByIdTest() {
    var relationshipId1 = "relationshipId1";
    var electronicAccess1 = new ElectronicAccess();
    electronicAccess1.setRelationshipId(relationshipId1);
    electronicAccess1.setUri("uri1");

    var relationshipId2 = "relationshipId2";
    var electronicAccess2 = new ElectronicAccess();
    electronicAccess2.setRelationshipId(relationshipId2);
    electronicAccess2.setUri("uri2");

    var electronicAccess3 = new ElectronicAccess();
    electronicAccess3.setRelationshipId(relationshipId1);
    electronicAccess3.setUri("uri3");

    when(relationshipClient.getById(relationshipId1)).thenThrow(new NotFoundException("error message"));
    when(relationshipClient.getById(relationshipId2)).thenThrow(new NotFoundException("error message"));

    var expected = "URL relationship;URI;Link text;Materials specified;URL public note\n" +
      "relationshipId1\u001F;uri1\u001F;\u001F;\u001F;\u001F|relationshipId2\u001F;uri2\u001F;\u001F;\u001F;\u001F|relationshipId1\u001F;uri3\u001F;\u001F;\u001F;";
    var actual = electronicAccessService.getElectronicAccessesToString(List.of(electronicAccess1, electronicAccess2, electronicAccess3), buildErrorServiceArgs(), HOLDINGS_RECORD, "tenant");

    assertEquals(expected, actual);
  }

  @Test
  void getElectronicAccessesToStringElectronicAccessRelationshipIsNullTest() {
    var electronicAccess = new ElectronicAccess();
    electronicAccess.setUri("uri");

    var expected = "URL relationship;URI;Link text;Materials specified;URL public note\n\u001F;uri\u001F;\u001F;\u001F;";
    var actual = electronicAccessService.getElectronicAccessesToString(List.of(electronicAccess), buildErrorServiceArgs(), HOLDINGS_RECORD,"tenant");

    assertEquals(expected, actual);
  }

  @Test
  void getRelationshipNameAndIdByIdTest() {
    var id = UUID.randomUUID().toString();
    var electronicAccessRelationship = new ElectronicAccessRelationship();
    electronicAccessRelationship.setId(id);
    electronicAccessRelationship.setName("name");

    when(relationshipClient.getById(id)).thenReturn(electronicAccessRelationship);

    var expected = electronicAccessRelationship.getName();
    var actual = electronicAccessService.getRelationshipNameById(id, buildErrorServiceArgs(), "tenant");

    verify(relationshipClient).getById(id);
    assertEquals(expected, actual);
  }

  @Test
  void getRelationshipNameAndIdByIdNotFoundExceptionTest() {
    Map<String, Collection<String>> headers = new HashMap<>();
    headers.put(X_OKAPI_TENANT,  List.of("original"));
    when(folioExecutionContext.getAllHeaders()).thenReturn(headers);
    var id = UUID.randomUUID().toString();
    var electronicAccessRelationship = new ElectronicAccessRelationship();
    electronicAccessRelationship.setId(id);
    electronicAccessRelationship.setName("name");

    when(relationshipClient.getById(id)).thenThrow(new NotFoundException("error message"));

    var expected = electronicAccessRelationship.getId();
    var actual = electronicAccessService.getRelationshipNameById(id, buildErrorServiceArgs(), "tenant");

    assertEquals(expected, actual);
  }

  private ErrorServiceArgs buildErrorServiceArgs() {
    return new ErrorServiceArgs(UUID.randomUUID().toString(), "identifier", "filename");
  }
}
