package org.folio.dew.batch.bulkedit.jobs;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.dew.domain.dto.IdentifierType;
import org.folio.dew.domain.dto.Instance;
import org.folio.dew.domain.dto.InstanceFormat;
import org.folio.dew.domain.dto.ErrorServiceArgs;
import org.folio.dew.domain.dto.InstanceContributorsInner;
import org.folio.dew.domain.dto.InstanceIdentifiersInner;
import org.folio.dew.domain.dto.InstanceSeriesInner;
import org.folio.dew.service.InstanceReferenceService;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.folio.dew.domain.dto.IdentifierType.fromValue;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.dew.domain.dto.eholdings.Identifier.TypeEnum.ISBN;
import static org.folio.dew.domain.dto.eholdings.Identifier.TypeEnum.ISSN;
import static org.folio.dew.utils.Constants.ARRAY_DELIMITER_SPACED;
import static org.folio.dew.utils.Constants.ITEM_DELIMITER;
import static org.folio.dew.utils.Constants.ITEM_DELIMITER_SPACED;

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class BulkEditInstanceProcessor implements ItemProcessor<Instance, InstanceFormat> {

  private final InstanceReferenceService  instanceReferenceService;


  @Value("#{jobParameters['identifierType']}")
  private String identifierType;
  @Value("#{jobParameters['jobId']}")
  private String jobId;
  @Value("#{jobParameters['fileName']}")
  private String fileName;

  @Override
  public InstanceFormat process(@NotNull Instance instance) {
    String identifierValue = getIdentifier(instance, identifierType);
    log.info("Identifier type={}, value={}", identifierType, identifierValue);
    var errorServiceArgs = new ErrorServiceArgs(jobId, identifierValue, FilenameUtils.getName(fileName));

    var instanceFormat = InstanceFormat.builder()
      .id(instance.getId())
      .discoverySuppress(isEmpty(instance.getVersion()) ? EMPTY : Boolean.toString(instance.getDiscoverySuppress()))
      .staffSuppress(isEmpty(instance.getStaffSuppress()) ? EMPTY : Boolean.toString(instance.getStaffSuppress()))
      .previouslyHeld(isEmpty(instance.getPreviouslyHeld()) ? EMPTY : Boolean.toString(instance.getPreviouslyHeld()))
      .hrid(instance.getHrid())
      .source(instance.getSource())
      .catalogedDate(formatDate(instance.getCatalogedDate()))
      .statusId(instanceReferenceService.getInstanceStatusNameById(instance.getStatusId(), errorServiceArgs))
      .modeOfIssuanceId(instanceReferenceService.getModeOfIssuanceNameById(instance.getModeOfIssuanceId(), errorServiceArgs))
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
      .isbn(IdentifierType.ISBN == fromValue(identifierType) ? identifierValue : null)
      .issn(IdentifierType.ISSN == fromValue(identifierType) ? identifierValue : null)
      .build();


    return instanceFormat.withOriginal(instance);
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


  private String getIdentifier(Instance instance, String identifierType) {
    try {
      return switch (fromValue(identifierType)) {
        case HRID -> instance.getHrid();
        case ISSN -> instance.getIdentifiers().stream()
          .filter(identifier -> instanceReferenceService.getTypeOfIdentifiersIdByName(ISSN.getValue()).equals(identifier.getIdentifierTypeId()))
          .findFirst()
          .map(InstanceIdentifiersInner::getValue)
          .orElse(instance.getId());
        case ISBN -> instance.getIdentifiers().stream()
          .filter(identifier -> instanceReferenceService.getTypeOfIdentifiersIdByName(ISBN.getValue()).equals(identifier.getIdentifierTypeId()))
          .findFirst()
          .map(InstanceIdentifiersInner::getValue)
          .orElse(instance.getId());

        default -> instance.getId();
      };
    } catch (IllegalArgumentException e) {
      return instance.getId();
    }
  }
}
