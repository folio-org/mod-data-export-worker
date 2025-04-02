package org.folio.dew.service.mapper;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dew.service.InstanceReferenceServiceCache.QUERY_PATTERN_ID;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.folio.dew.BaseBatchTest;
import org.folio.dew.domain.dto.ExtendedInstance;
import org.folio.dew.domain.dto.Instance;
import org.folio.dew.domain.dto.InstanceNoteType;
import org.folio.dew.domain.dto.InstanceNotesInner;
import org.folio.dew.domain.dto.InstanceSubjectsInner;
import org.folio.dew.domain.dto.SubjectSource;
import org.folio.dew.domain.dto.SubjectSourceCollection;
import org.folio.dew.domain.dto.SubjectType;
import org.folio.dew.domain.dto.SubjectTypeCollection;
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

  @Test
  void shouldMapInstanceSubject() {
    var subjectSourceId1 = "0616df94-5b81-4b8a-8b25-1d2ee7292148" ;
    var subjectTypeId1 = "683368ff-b213-4041-b89f-c39f4d851e3b";
    var subjectSourceId2 = "136ffb8c-6f12-450a-92a9-122bee32cd3f";
    var subjectTypeId2 = "f6ec476c-6261-436c-ba92-15c35307bc0a";
    var instance = new Instance()
        .id(UUID.randomUUID().toString())
        .subjects(Set.of(
            new InstanceSubjectsInner()
                .value("Subject#1")
                .sourceId(subjectSourceId1)
                .typeId(subjectTypeId1),
            new InstanceSubjectsInner()
                .value("Subject#2")
                .sourceId(subjectSourceId2)
                .typeId(subjectTypeId2)));
    var mapper = new InstanceMapper(instanceReferenceService, new SpecialCharacterEscaper(), electronicAccessService);

    when(subjectSourceClient.getByQuery(format(QUERY_PATTERN_ID, subjectSourceId1)))
        .thenReturn(new SubjectSourceCollection().subjectSources(List.of(new SubjectSource().id(subjectSourceId1).name("Source#1"))));
    when(subjectSourceClient.getByQuery(format(QUERY_PATTERN_ID, subjectSourceId2)))
        .thenReturn(new SubjectSourceCollection().subjectSources(List.of(new SubjectSource().id(subjectSourceId2).name("Source#2"))));
    when(subjectTypeClient.getByQuery(format(QUERY_PATTERN_ID, subjectTypeId1)))
        .thenReturn(new SubjectTypeCollection().subjectTypes(List.of(new SubjectType().id(subjectTypeId1).name("Type#1"))));
    when(subjectTypeClient.getByQuery(format(QUERY_PATTERN_ID, subjectTypeId2)))
        .thenReturn(new SubjectTypeCollection().subjectTypes(List.of(new SubjectType().id(subjectTypeId1).name("Type#2"))));

    var instanceFormat = mapper.mapToInstanceFormat(new ExtendedInstance().entity(instance).tenantId("diku"), "identifier", UUID.randomUUID().toString(), "errorFile");
    assertThat(instanceFormat.getSubject()).contains("Subject#1;Source#1;Type#1");
    assertThat(instanceFormat.getSubject()).contains("Subject#2;Source#2;Type#2");

  }
}
