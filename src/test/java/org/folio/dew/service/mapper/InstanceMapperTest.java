package org.folio.dew.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.folio.dew.BaseBatchTest;
import org.folio.dew.client.InstanceNoteTypesClient;
import org.folio.dew.domain.dto.Instance;
import org.folio.dew.domain.dto.InstanceNoteType;
import org.folio.dew.domain.dto.InstanceNotesInner;
import org.folio.dew.service.InstanceReferenceService;
import org.folio.dew.service.SpecialCharacterEscaper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Collections;
import java.util.UUID;

class InstanceMapperTest extends BaseBatchTest {

  @Autowired
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
}
