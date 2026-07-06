package org.folio.dew.batch.acquisitions.services;

import org.folio.dew.client.CustomFieldsClient;
import org.folio.dew.domain.dto.acquisitions.customfields.CustomField;
import org.folio.dew.domain.dto.acquisitions.customfields.CustomFieldCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.HttpClientErrorException;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class CustomFieldDefinitionServiceTest {

  @Mock
  private CustomFieldsClient customFieldsClient;

  @InjectMocks
  private CustomFieldDefinitionService service;

  @Test
  void getDefinitionsByRefId_indexesByRefId_andQueriesTargetModule() {
    var collection = new CustomFieldCollection();
    collection.setCustomFields(List.of(customField("a"), customField("b")));
    when(customFieldsClient.getCustomFields(anyString(), anyInt(), anyString())).thenReturn(collection);

    var result = service.getDefinitionsByRefId("po_line");

    assertThat(result).containsOnlyKeys("a", "b");

    var queryCaptor = ArgumentCaptor.forClass(String.class);
    var moduleCaptor = ArgumentCaptor.forClass(String.class);
    org.mockito.Mockito.verify(customFieldsClient).getCustomFields(queryCaptor.capture(), anyInt(), moduleCaptor.capture());
    assertThat(queryCaptor.getValue()).isEqualTo("entityType==po_line");
    assertThat(moduleCaptor.getValue()).isEqualTo("mod-orders-storage");
  }

  @Test
  void getDefinitionsByRefId_skipsBlankRefIds() {
    var collection = new CustomFieldCollection();
    collection.setCustomFields(new ArrayList<>(List.of(customField("a"), customField(""), customField(null))));
    when(customFieldsClient.getCustomFields(anyString(), anyInt(), anyString())).thenReturn(collection);

    var result = service.getDefinitionsByRefId("po_line");

    assertThat(result).containsOnlyKeys("a");
  }

  @Test
  void getDefinitionsByRefId_nullCollection_returnsEmpty() {
    when(customFieldsClient.getCustomFields(anyString(), anyInt(), anyString())).thenReturn(new CustomFieldCollection());

    assertThat(service.getDefinitionsByRefId("po_line")).isEmpty();
  }

  @Test
  void getDefinitionsByRefId_clientThrows_degradesToEmptyMap() {
    when(customFieldsClient.getCustomFields(anyString(), anyInt(), eq("mod-orders-storage")))
      .thenThrow(HttpClientErrorException.create(NOT_FOUND, "Not Found", null, null, null));

    assertThat(service.getDefinitionsByRefId("po_line")).isEmpty();
  }

  private static CustomField customField(String refId) {
    var cf = new CustomField();
    cf.setRefId(refId);
    cf.setName("name-" + refId);
    return cf;
  }
}