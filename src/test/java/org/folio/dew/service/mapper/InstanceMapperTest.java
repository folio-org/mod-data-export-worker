package org.folio.dew.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.folio.dew.BaseBatchTest;
import org.folio.dew.domain.dto.ExtendedInstance;
import org.folio.dew.domain.dto.Instance;
import org.folio.dew.domain.dto.InstanceNoteType;
import org.folio.dew.domain.dto.InstanceNotesInner;
import org.folio.dew.service.ElectronicAccessService;
import org.folio.dew.service.InstanceReferenceService;
import org.folio.dew.service.SpecialCharacterEscaper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

class InstanceMapperTest extends BaseBatchTest {

  @Autowired
  private InstanceReferenceService instanceReferenceService;
  @Autowired
  private ElectronicAccessService electronicAccessService;

  @Test
  void shouldMapInstanceNoteTypes() {
    var noteTypeId = UUID.randomUUID().toString();
    var instance = new Instance()
      .id(UUID.randomUUID().toString())
      .notes(Collections.singletonList(new InstanceNotesInner()
        .instanceNoteTypeId(noteTypeId)
        .note("test note")
        .staffOnly(true)));
    var mapper = new InstanceMapper(instanceReferenceService, new SpecialCharacterEscaper(), electronicAccessService);
    when(instanceNoteTypesClient.getNoteTypeById(noteTypeId))
      .thenReturn(new InstanceNoteType().name("note type"));

    var instanceFormat = mapper.mapToInstanceFormat(new ExtendedInstance().entity(instance).tenantId("diku"), "identifier", UUID.randomUUID().toString(), "errorFile");

    assertThat(instanceFormat.getNotes()).isEqualTo("note type;test note;true");
  }

  @Test
  void shouldMapInstanceStatisticalCodes() {
    var statisticalCodeId1 = "b5968c9e-cddc-4576-99e3-8e60aed8b0dd";
    var statisticalCodeId2 = "b5968c9e-cddc-4576-99e3-8e60aed8b0dd";
    var instance = new Instance()
      .id(UUID.randomUUID().toString())
      .statisticalCodeIds(List.of(statisticalCodeId1, statisticalCodeId2));
    var mapper = new InstanceMapper(instanceReferenceService, new SpecialCharacterEscaper(), electronicAccessService);

    var instanceFormat = mapper.mapToInstanceFormat(new ExtendedInstance().entity(instance).tenantId("diku"), "identifier", UUID.randomUUID().toString(), "errorFile");

    assertThat(instanceFormat.getStatisticalCode()).isEqualTo("Code Type 1 (CD): books - Book, print (books) | Code Type 1 (CD): books - Book, print (books)");
  }
}
