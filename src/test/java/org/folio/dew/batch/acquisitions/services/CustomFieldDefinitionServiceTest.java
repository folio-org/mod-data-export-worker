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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class CustomFieldDefinitionServiceTest {

  private static final String MODULE_ID = "mod-orders-storage-14.0.0-SNAPSHOT.498";

  @Mock
  private CustomFieldsClient customFieldsClient;
  @Mock
  private OrdersStorageModuleIdResolver ordersStorageModuleIdResolver;

  @InjectMocks
  private CustomFieldDefinitionService service;

  @Test
  void getDefinitionsByRefId_indexesByRefId_andPassesResolvedModuleId() {
    when(ordersStorageModuleIdResolver.resolve()).thenReturn(MODULE_ID);
    var collection = new CustomFieldCollection();
    collection.setCustomFields(List.of(customField("a"), customField("b")));
    when(customFieldsClient.getCustomFields(anyString(), anyInt(), anyString())).thenReturn(collection);

    var result = service.getDefinitionsByRefId("po_line");

    assertThat(result).containsOnlyKeys("a", "b");
    var queryCaptor = ArgumentCaptor.forClass(String.class);
    var moduleCaptor = ArgumentCaptor.forClass(String.class);
    verify(customFieldsClient).getCustomFields(queryCaptor.capture(), anyInt(), moduleCaptor.capture());
    assertThat(queryCaptor.getValue()).isEqualTo("entityType==po_line");
    assertThat(moduleCaptor.getValue()).isEqualTo(MODULE_ID);
  }

  @Test
  void getDefinitionsByRefId_skipsBlankRefIds() {
    when(ordersStorageModuleIdResolver.resolve()).thenReturn(MODULE_ID);
    var collection = new CustomFieldCollection();
    collection.setCustomFields(new ArrayList<>(List.of(customField("a"), customField(""), customField(null))));
    when(customFieldsClient.getCustomFields(anyString(), anyInt(), anyString())).thenReturn(collection);

    assertThat(service.getDefinitionsByRefId("po_line")).containsOnlyKeys("a");
  }

  @Test
  void getDefinitionsByRefId_nullCollection_returnsEmpty() {
    when(ordersStorageModuleIdResolver.resolve()).thenReturn(MODULE_ID);
    when(customFieldsClient.getCustomFields(anyString(), anyInt(), anyString())).thenReturn(new CustomFieldCollection());

    assertThat(service.getDefinitionsByRefId("po_line")).isEmpty();
  }

  @Test
  void getDefinitionsByRefId_moduleIdUnresolved_skipsCallAndReturnsEmpty() {
    when(ordersStorageModuleIdResolver.resolve()).thenReturn(null);

    assertThat(service.getDefinitionsByRefId("po_line")).isEmpty();
    verify(customFieldsClient, never()).getCustomFields(anyString(), anyInt(), anyString());
  }

  @Test
  void getDefinitionsByRefId_clientThrows_degradesToEmptyMap() {
    when(ordersStorageModuleIdResolver.resolve()).thenReturn(MODULE_ID);
    when(customFieldsClient.getCustomFields(anyString(), anyInt(), eq(MODULE_ID)))
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
