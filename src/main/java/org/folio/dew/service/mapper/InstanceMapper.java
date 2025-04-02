package org.folio.dew.service.mapper;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.dew.domain.dto.EntityType.INSTANCE;
import static org.folio.dew.utils.BulkEditProcessorHelper.booleanToStringNullSafe;
import static org.folio.dew.utils.Constants.ARRAY_DELIMITER;
import static org.folio.dew.utils.Constants.ARRAY_DELIMITER_SPACED;
import static org.folio.dew.utils.Constants.EMPTY_ELEMENT;
import static org.folio.dew.utils.Constants.VERTICAL_BAR_WITH_HIDDEN_SYMBOL;
import static org.folio.dew.utils.Constants.ITEM_DELIMITER_SPACED;
import static org.folio.dew.utils.Constants.KEY_VALUE_DELIMITER;
import static org.folio.dew.utils.Constants.STATISTICAL_CODE_NAME_SEPARATOR;
import static org.folio.dew.utils.DateTimeHelper.formatDate;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.ElectronicAccess;
import org.folio.dew.domain.dto.ErrorServiceArgs;
import org.folio.dew.domain.dto.ExtendedInstance;
import org.folio.dew.domain.dto.InstanceContributorsInner;
import org.folio.dew.domain.dto.InstanceElectronicAccessInner;
import org.folio.dew.domain.dto.InstanceFormat;
import org.folio.dew.domain.dto.InstanceNotesInner;
import org.folio.dew.domain.dto.InstanceSeriesInner;
import org.folio.dew.domain.dto.InstanceSubjectsInner;
import org.folio.dew.service.ElectronicAccessService;
import org.folio.dew.service.InstanceReferenceService;
import org.folio.dew.service.SpecialCharacterEscaper;
import org.folio.dew.utils.NonEmpty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Log4j2
public class InstanceMapper {
  private final InstanceReferenceService instanceReferenceService;
  private final SpecialCharacterEscaper specialCharacterEscaper;
  private final ElectronicAccessService electronicAccessService;

  public InstanceFormat mapToInstanceFormat(ExtendedInstance extendedInstance, String identifier, String jobId, String errorFileName) {
    var errorServiceArgs = new ErrorServiceArgs(jobId, identifier, errorFileName);
    var tenantId = extendedInstance.getTenantId();
    var instance = extendedInstance.getEntity();
    return InstanceFormat.builder()
      .id(instance.getId())
      .discoverySuppress(isEmpty(instance.getVersion()) ? EMPTY : Boolean.toString(instance.getDiscoverySuppress()))
      .staffSuppress(isEmpty(instance.getStaffSuppress()) ? EMPTY : Boolean.toString(instance.getStaffSuppress()))
      .previouslyHeld(isEmpty(instance.getPreviouslyHeld()) ? EMPTY : Boolean.toString(instance.getPreviouslyHeld()))
      .hrid(instance.getHrid())
      .source(instance.getSource())
      .catalogedDate(formatDate(instance.getCatalogedDate()))
      .statusId(instanceReferenceService.getInstanceStatusNameById(instance.getStatusId(), errorServiceArgs))
      .modeOfIssuanceId(instanceReferenceService.getModeOfIssuanceNameById(instance.getModeOfIssuanceId(), errorServiceArgs))
      .statisticalCode(getStatisticalCodeNames(instance.getStatisticalCodeIds(), errorServiceArgs))
      .notes(fetchNotes(instance.getNotes(), errorServiceArgs))
      .administrativeNotes(isEmpty(instance.getAdministrativeNotes()) ? EMPTY : String.join(ITEM_DELIMITER_SPACED, instance.getAdministrativeNotes()))
      .title(instance.getTitle())
      .indexTitle(instance.getIndexTitle())
      .series(fetchSeries(instance.getSeries()))
      .contributors(fetchContributorNames(instance.getContributors()))
      .editions(isEmpty(instance.getEditions()) ? EMPTY : String.join(ITEM_DELIMITER_SPACED, new ArrayList<>(instance.getEditions())))
      .physicalDescriptions(isEmpty(instance.getPhysicalDescriptions()) ? EMPTY : String.join(ITEM_DELIMITER_SPACED, instance.getPhysicalDescriptions()))
      .instanceTypeId(instanceReferenceService.getInstanceTypeNameById(instance.getInstanceTypeId(), errorServiceArgs))
      .natureOfContentTermIds(fetchNatureOfContentTerms(instance.getNatureOfContentTermIds(), errorServiceArgs))
      .instanceFormatIds(fetchInstanceFormats(instance.getInstanceFormatIds(), errorServiceArgs))
      .languages(isEmpty(instance.getLanguages()) ? EMPTY : String.join(ITEM_DELIMITER_SPACED, instance.getLanguages()))
      .publicationFrequency(isEmpty(instance.getPublicationFrequency()) ? EMPTY : String.join(ITEM_DELIMITER_SPACED, new ArrayList<>(instance.getPublicationFrequency())))
      .publicationRange(isEmpty(instance.getPublicationRange()) ? EMPTY : String.join(ITEM_DELIMITER_SPACED, new ArrayList<>(instance.getPublicationRange())))
      .electronicAccess(electronicAccessService.getElectronicAccessesToString(toElectronicAccesses(instance.getElectronicAccess()), errorServiceArgs,INSTANCE, tenantId))
      .subject(fetchSubjects(instance.getSubjects(), errorServiceArgs))
      .build();
  }

  private String fetchInstanceFormats(List<String> instanceFormats, ErrorServiceArgs errorServiceArgs) {
    return isEmpty(instanceFormats) ? EMPTY :
      instanceFormats.stream()
        .map(iFormatId -> instanceReferenceService.getFormatOfInstanceNameById(iFormatId, errorServiceArgs))
        .collect(Collectors.joining(ITEM_DELIMITER_SPACED));
  }

  private String fetchNatureOfContentTerms(List<String> natureOfContentTermIds, ErrorServiceArgs errorServiceArgs) {
    return isEmpty(natureOfContentTermIds) ? EMPTY :
      natureOfContentTermIds.stream()
        .map(natId -> instanceReferenceService.getNatureOfContentTermNameById(natId, errorServiceArgs))
        .collect(Collectors.joining(ITEM_DELIMITER_SPACED));
  }

  private String fetchContributorNames(List<InstanceContributorsInner> contributors) {
    return isEmpty(contributors) ? EMPTY :
      contributors.stream()
        .map(InstanceContributorsInner::getName)
        .collect(Collectors.joining(ARRAY_DELIMITER_SPACED));
  }

  private String fetchSeries(Set<InstanceSeriesInner> series) {
    return isEmpty(series) ? EMPTY :
        series.stream()
          .map(InstanceSeriesInner::getValue)
          .collect(Collectors.joining(ITEM_DELIMITER_SPACED));
  }

  private String fetchNotes(List<InstanceNotesInner> notes, ErrorServiceArgs errorServiceArgs) {
    return isEmpty(notes) ? EMPTY :
      notes.stream()
        .map(note -> noteToString(note, errorServiceArgs))
        .collect(Collectors.joining(ITEM_DELIMITER_SPACED));
  }

  private String fetchSubjects(Set<InstanceSubjectsInner> subjects, ErrorServiceArgs errorServiceArgs) {
    return isEmpty(subjects) ? EMPTY :
        subjects.stream()
            .map(subject -> subjectToString(subject, errorServiceArgs))
            .collect(Collectors.joining(VERTICAL_BAR_WITH_HIDDEN_SYMBOL));
  }

  private String noteToString(InstanceNotesInner note, ErrorServiceArgs errorServiceArgs) {
    return (String.join(ARRAY_DELIMITER,
      specialCharacterEscaper.escape(instanceReferenceService.getInstanceNoteTypeNameById(note.getInstanceNoteTypeId(), errorServiceArgs)),
      specialCharacterEscaper.escape(note.getNote()),
      booleanToStringNullSafe(note.getStaffOnly())));
  }

  private String subjectToString(InstanceSubjectsInner subject, ErrorServiceArgs errorServiceArgs) {
    return (String.join(ARRAY_DELIMITER,
        NonEmpty.of(subject.getValue()).orElse(EMPTY_ELEMENT),
        NonEmpty.of( instanceReferenceService.getSubjectSourceNameById(subject.getSourceId(), errorServiceArgs)).orElse(
            EMPTY_ELEMENT),
        NonEmpty.of( instanceReferenceService.getSubjectTypeNameById(subject.getTypeId(), errorServiceArgs)).orElse(
            EMPTY_ELEMENT))
    );
  }

  private String getStatisticalCodeNames(List<String> codeIds, ErrorServiceArgs args) {
    return isEmpty(codeIds) ? EMPTY : codeIds.stream()
      .filter(Objects::nonNull)
      .map(id -> getStatisticalCodeFormat(id, args))
      .map(specialCharacterEscaper::escapeStatisticalCode)
      .collect(Collectors.joining(ITEM_DELIMITER_SPACED));
  }

  private String getStatisticalCodeFormat(String id, ErrorServiceArgs args) {
    var typeName = instanceReferenceService.getStatisticalCodeTypeNameById(id, args);
    var code = instanceReferenceService.getStatisticalCodeCodeById(id, args);
    var name = instanceReferenceService.getStatisticalCodeNameById(id, args);
    return String.format("%s%s %s%s%s", typeName, KEY_VALUE_DELIMITER, code, STATISTICAL_CODE_NAME_SEPARATOR, name);
  }

  private List<ElectronicAccess> toElectronicAccesses(List<InstanceElectronicAccessInner> listInstElAcc) {
    return Stream.ofNullable(listInstElAcc).flatMap(List::stream).map(this::toElectronicAccess).toList();
  }

  private ElectronicAccess toElectronicAccess(InstanceElectronicAccessInner instanceElAcc) {
    return new ElectronicAccess().linkText(instanceElAcc.getLinkText()).uri(instanceElAcc.getUri())
      .materialsSpecification(instanceElAcc.getMaterialsSpecification()).publicNote(instanceElAcc.getPublicNote())
      .relationshipId(instanceElAcc.getRelationshipId());
  }
}
