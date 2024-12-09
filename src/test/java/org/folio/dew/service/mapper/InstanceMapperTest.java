package org.folio.dew.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.folio.dew.client.InstanceNoteTypesClient;
import org.folio.dew.client.StatisticalCodeClient;
import org.folio.dew.domain.dto.Instance;
import org.folio.dew.domain.dto.InstanceNoteType;
import org.folio.dew.domain.dto.InstanceNotesInner;
import org.folio.dew.domain.dto.StatisticalCode;
import org.folio.dew.service.InstanceReferenceService;
import org.folio.dew.service.SpecialCharacterEscaper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class InstanceMapperTest {
  @Mock
  private InstanceNoteTypesClient instanceNoteTypesClient;
  @Mock
  private StatisticalCodeClient statisticalCodeClient;
  @InjectMocks
  private InstanceReferenceService instanceReferenceService;

  @Test
  void shouldMapInstanceNoteTypes() {
    var noteTypeId = UUID.randomUUID().toString();
    var instance = new Instance()
      .id(UUID.randomUUID().toString())
      .notes(Collections.singletonList(new InstanceNotesInner()
        .instanceNoteTypeId(noteTypeId)
        .note("test note")
        .staffOnly(true)));
    var mapper = new InstanceMapper(instanceReferenceService, new SpecialCharacterEscaper());
    when(instanceNoteTypesClient.getNoteTypeById(noteTypeId))
      .thenReturn(new InstanceNoteType().name("note type"));

    var instanceFormat = mapper.mapToInstanceFormat(instance, "identifier", UUID.randomUUID().toString(), "errorFile");

    assertThat(instanceFormat.getNotes()).isEqualTo("note type;test note;true");
  }

  @Test
  void shouldMapInstanceStatisticalCodes() {
    var statisticalCodeId1 = UUID.randomUUID().toString();
    var statisticalCodeId2 = UUID.randomUUID().toString();
    var instance = new Instance()
      .id(UUID.randomUUID().toString())
      .statisticalCodeIds(List.of(statisticalCodeId1, statisticalCodeId2));
    var mapper = new InstanceMapper(instanceReferenceService, new SpecialCharacterEscaper());

    when(statisticalCodeClient.getById(statisticalCodeId1))
      .thenReturn(new StatisticalCode().name("statistical_cod_1"));
    when(statisticalCodeClient.getById(statisticalCodeId2))
      .thenReturn(new StatisticalCode().name("statistical_code_2"));

    var instanceFormat = mapper.mapToInstanceFormat(instance, "identifier", UUID.randomUUID().toString(), "errorFile");

    assertThat(instanceFormat.getStatisticalCodes()).isEqualTo("statistical_cod_1;statistical_code_2");
  }
}
