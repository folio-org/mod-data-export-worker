package org.folio.dew.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.folio.dew.client.InstanceNoteTypesClient;
import org.folio.dew.domain.dto.Instance;
import org.folio.dew.domain.dto.InstanceNoteType;
import org.folio.dew.domain.dto.InstanceNotesInner;
import org.folio.dew.service.InstanceReferenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class InstanceMapperTest {
  @Mock
  private InstanceNoteTypesClient instanceNoteTypesClient;
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
    var mapper = new InstanceMapper(instanceReferenceService);
    when(instanceNoteTypesClient.getNoteTypeById(noteTypeId))
      .thenReturn(new InstanceNoteType().name("note type"));

    var instanceFormat = mapper.mapToInstanceFormat(instance, "identifier", UUID.randomUUID().toString(), "errorFile");

    assertThat(instanceFormat.getNotes()).isEqualTo("note type;test note;true");
  }
}
