package org.folio.dew.service.mapper;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.dew.utils.BulkEditProcessorHelper.booleanToStringNullSafe;
import static org.folio.dew.utils.Constants.ARRAY_DELIMITER;
import static org.folio.dew.utils.Constants.ARRAY_DELIMITER_SPACED;
import static org.folio.dew.utils.Constants.ITEM_DELIMITER_SPACED;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.ErrorServiceArgs;
import org.folio.dew.domain.dto.Instance;
import org.folio.dew.domain.dto.InstanceContributorsInner;
import org.folio.dew.domain.dto.InstanceFormat;
import org.folio.dew.domain.dto.InstanceNotesInner;
import org.folio.dew.domain.dto.InstanceSeriesInner;
import org.folio.dew.service.InstanceReferenceService;
import org.folio.dew.service.SpecialCharacterEscaper;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class InstanceMapper {
  private final InstanceReferenceService instanceReferenceService;
  private final SpecialCharacterEscaper specialCharacterEscaper;

  public InstanceFormat mapToInstanceFormat(Instance instance, String identifier, String jobId, String errorFileName) {
    var errorServiceArgs = new ErrorServiceArgs(jobId, identifier, errorFileName);
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
      .build();
  }

  private String formatDate(String catalogedDateInput) {
    if (isEmpty(catalogedDateInput)){
      return EMPTY;
    }

    var inputFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    var outputFormatter = new SimpleDateFormat("yyyy-MM-dd");

    Date date;
    try {
      date = inputFormatter.parse(catalogedDateInput);
    } catch (ParseException e) {
      log.error("Can't parse catalogedDate {}.", catalogedDateInput);
      return catalogedDateInput;
    }
    return outputFormatter.format(date);
  }

  private String fetchInstanceFormats(List<String> instanceFormats, ErrorServiceArgs errorServiceArgs) {
    return isEmpty(instanceFormats) ? EMPTY :
      instanceFormats.stream()
        .map(iFormatId -> instanceReferenceService.getFormatOfInstanceNameById(iFormatId, errorServiceArgs))
        .collect(Collectors.joining(ITEM_DELIMITER_SPACED));
  }

  private String fetchNatureOfContentTerms(Set<String> natureOfContentTermIds, ErrorServiceArgs errorServiceArgs) {
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

  private String noteToString(InstanceNotesInner note, ErrorServiceArgs errorServiceArgs) {
    return (String.join(ARRAY_DELIMITER,
      specialCharacterEscaper.escape(instanceReferenceService.getInstanceNoteTypeNameById(note.getInstanceNoteTypeId(), errorServiceArgs)),
      specialCharacterEscaper.escape(note.getNote()),
      booleanToStringNullSafe(note.getStaffOnly())));
  }
}
